package com.olab.studyqb.data.repository

import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.network.groq.GroqApi
import com.olab.studyqb.data.network.groq.GroqPrompts
import com.olab.studyqb.data.network.groq.dto.ChatCompletionRequestDto
import com.olab.studyqb.data.network.groq.dto.ChatMessageDto
import com.olab.studyqb.domain.CategorySentenceStarters
import com.olab.studyqb.domain.SubjectTypes
import kotlinx.coroutines.CancellationException

data class TossupResult(val text: String, val usedFallback: Boolean)

class TossupRepository(
    private val groqApi: GroqApi,
    private val cardRepository: CardRepository
) {
    /**
     * Reuses `card.cachedTossup` if present (no network call, matching iOS's "only generated
     * once per card" caching). Otherwise generates via two sequential Groq calls, caching the
     * result on success. On any failure at either step, falls back to a deterministic
     * per-category rule-based tossup — which is intentionally NOT cached, so a later
     * online run can still produce the real LLM version.
     */
    suspend fun getOrGenerateTossup(card: CardEntity): TossupResult {
        if (!card.cachedTossup.isNullOrBlank()) {
            return TossupResult(card.cachedTossup, usedFallback = false)
        }

        return try {
            val subjectResponse = groqApi.chatCompletion(
                ChatCompletionRequestDto(
                    model = GroqPrompts.TOSSUP_MODEL,
                    messages = listOf(
                        ChatMessageDto(role = "user", content = GroqPrompts.subjectClassificationPrompt(card.answer, card.category.displayName))
                    )
                )
            )
            val rawSubject = GroqPrompts.stripReasoningTags(subjectResponse.choices.firstOrNull()?.message?.content.orEmpty())
            val subjectType = SubjectTypes.match(rawSubject) ?: "topic"

            val reversedClues = card.personalNotes
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .asReversed()

            val stitchResponse = groqApi.chatCompletion(
                ChatCompletionRequestDto(
                    model = GroqPrompts.TOSSUP_MODEL,
                    messages = listOf(
                        ChatMessageDto(
                            role = "user",
                            content = GroqPrompts.tossupStitchingPrompt(card.category.displayName, card.answer, subjectType, reversedClues)
                        )
                    )
                )
            )
            val tossup = GroqPrompts.stripReasoningTags(stitchResponse.choices.firstOrNull()?.message?.content.orEmpty())

            cardRepository.updateCachedTossup(card, tossup)
            TossupResult(tossup, usedFallback = false)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            val fallback = CategorySentenceStarters.buildFallbackTossup(card.category, card.personalNotes)
            TossupResult(fallback, usedFallback = true)
        }
    }
}
