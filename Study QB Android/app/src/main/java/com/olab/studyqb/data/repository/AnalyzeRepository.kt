package com.olab.studyqb.data.repository

import com.olab.studyqb.data.local.CachedClueDataSource
import com.olab.studyqb.data.network.NetworkErrors
import com.olab.studyqb.data.network.groq.GroqApi
import com.olab.studyqb.data.network.groq.GroqModelRotator
import com.olab.studyqb.data.network.groq.GroqPrompts
import com.olab.studyqb.data.network.groq.dto.ChatCompletionRequestDto
import com.olab.studyqb.data.network.groq.dto.ChatMessageDto
import com.olab.studyqb.data.network.qbreader.QbReaderApi
import com.olab.studyqb.data.network.qbreader.dto.FrequencyEntryDto
import com.olab.studyqb.data.network.qbreader.dto.TossupDto
import com.olab.studyqb.domain.Category
import kotlinx.coroutines.CancellationException

sealed interface AnalyzeOutcome {
    data class Success(val responseText: String, val questionCount: Int, val modelUsed: String) : AnalyzeOutcome
    data object NoResults : AnalyzeOutcome
    data class Error(val message: String) : AnalyzeOutcome
}

class AnalyzeRepository(
    private val qbReaderApi: QbReaderApi,
    private val groqApi: GroqApi,
    private val modelRotator: GroqModelRotator,
    private val cachedClues: CachedClueDataSource
) {
    suspend fun fetchQuestions(query: String, difficulties: Set<Int>, categories: Set<Category>): List<TossupDto> {
        val response = qbReaderApi.query(
            // Retrofit's default (non-`encoded`) @Query already percent-encodes this (verified
            // live: spaces become %20) — do not pre-encode here, or it gets double-encoded.
            queryString = query,
            difficulties = difficultiesParam(difficulties),
            categories = categoriesParam(categories)
        )
        return response.tossups.questionArray
    }

    suspend fun fetchFrequencyList(category: Category, difficulties: Set<Int>): List<FrequencyEntryDto> {
        val response = qbReaderApi.frequencyList(
            category = category.displayName,
            difficulties = difficultiesParam(difficulties)
        )
        return response.frequencyList
    }

    suspend fun analyzeClues(answerQuery: String, difficulties: Set<Int>, categories: Set<Category>): AnalyzeOutcome {
        // Bundled dataset first, matching AnalyzeView.swift, which only calls the model when
        // `lookupCachedQuery` misses. A hit costs no network round trip and no Groq quota.
        cachedClues.lookup(answerQuery, categories)?.let { cached ->
            return AnalyzeOutcome.Success(
                responseText = cached.details,
                questionCount = cached.num_questions_analyzed,
                modelUsed = cached.model
            )
        }

        val questions = try {
            fetchQuestions(answerQuery, difficulties, categories)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            return AnalyzeOutcome.Error(NetworkErrors.userMessage(t))
        }
        if (questions.isEmpty()) return AnalyzeOutcome.NoResults

        val prompt = GroqPrompts.clueAnalysisPrompt(answerQuery, questions)

        repeat(modelRotator.models.size) {
            val model = modelRotator.currentModel()
            try {
                val response = groqApi.chatCompletion(
                    ChatCompletionRequestDto(model = model, messages = listOf(ChatMessageDto(role = "user", content = prompt)))
                )
                val content = response.choices.firstOrNull()?.message?.content.orEmpty()
                return AnalyzeOutcome.Success(
                    responseText = GroqPrompts.stripReasoningTags(content),
                    questionCount = questions.size,
                    modelUsed = model
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                if (GroqModelRotator.isRateLimited(t)) {
                    modelRotator.advance()
                } else {
                    return AnalyzeOutcome.Error(NetworkErrors.userMessage(t))
                }
            }
        }
        return AnalyzeOutcome.Error(NetworkErrors.RATE_LIMITED)
    }

    private fun difficultiesParam(difficulties: Set<Int>): String =
        (if (difficulties.isEmpty()) (0..10).toList() else difficulties.sorted()).joinToString(",")

    private fun categoriesParam(categories: Set<Category>): String? =
        if (categories.isEmpty() || categories.size == Category.all.size) null
        else categories.joinToString(",") { it.displayName }
}
