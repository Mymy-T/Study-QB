package com.olab.studyqb.ui.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.network.qbreader.dto.FrequencyEntryDto
import com.olab.studyqb.data.repository.AnalyzeOutcome
import com.olab.studyqb.data.repository.AnalyzeRepository
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.CardMatching
import com.olab.studyqb.domain.Difficulty
import com.olab.studyqb.domain.SuggestionPicker
import com.olab.studyqb.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyzeUiState(
    val allCards: List<CardEntity> = emptyList(),
    val categories: Set<Category> = Category.all.toSet(),
    val difficulties: Set<Int> = Difficulty.highSchool,
    val queryText: String = "",
    val isLoading: Boolean = false,
    val responseText: String? = null,
    val responseReceived: Boolean = false,
    val questionCount: Int = 0,
    val modelUsed: String? = null,
    val showEmptyQueryAlert: Boolean = false,
    val showHelp: Boolean = false,
    val showFreqList: Boolean = false,
    val freqListLoading: Boolean = false,
    val freqListItems: List<FrequencyEntryDto> = emptyList(),
    val awaitingSuggestion: Boolean = false,
    val showNoSuggestionAlert: Boolean = false,
    /**
     * The category a card saved straight out of Analyze should open with, or null to let the
     * card editor pick its own default. iOS's `answerCategory`, added 2026-10-19.
     */
    val answerCategory: Category? = null,
    /** Whether [queryText] is still the untouched answer that "Suggest" put there. */
    val validSuggestionMade: Boolean = false,
    /** The answer "Suggest" handed out last, so the next tap can avoid repeating it. */
    val lastSuggestedAnswer: String = ""
)

/**
 * Which category a card saved straight out of Analyze should open with — the `answerCategory`
 * bookkeeping from AnalyzeView.swift's `analyzeClues()`, pulled out as a pure function so it
 * can be tested without a ViewModel.
 *
 * A suggestion knows the category it came from, and that wins for as long as the suggested text
 * is intact. Failing that, the only unambiguous signal is a category filter narrowed to exactly
 * one entry. Anything else would be a guess, so it returns null and the editor keeps its own
 * default (Literature, matching iOS's `answerCategory ?? .lit`).
 */
fun resolveAnswerCategory(
    validSuggestion: Boolean,
    suggestionCategory: Category?,
    selected: Set<Category>
): Category? = when {
    validSuggestion -> suggestionCategory
    selected.size == 1 -> selected.first()
    else -> null
}

class AnalyzeViewModel(
    private val repository: AnalyzeRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val optionsState = MutableStateFlow(AnalyzeUiState())

    val uiState: StateFlow<AnalyzeUiState> =
        combine(cardRepository.observeAll(), optionsState) { cards, options ->
            options.copy(allCards = cards)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyzeUiState())

    fun setCategories(categories: Set<Category>) = optionsState.update { it.copy(categories = categories) }
    fun setDifficulties(difficulties: Set<Int>) = optionsState.update { it.copy(difficulties = difficulties) }
    fun setQueryText(text: String) = optionsState.update { it.copy(queryText = text) }
    fun dismissEmptyQueryAlert() = optionsState.update { it.copy(showEmptyQueryAlert = false) }
    fun setShowHelp(show: Boolean) = optionsState.update { it.copy(showHelp = show) }
    fun dismissFreqList() = optionsState.update { it.copy(showFreqList = false) }
    fun dismissNoSuggestionAlert() = optionsState.update { it.copy(showNoSuggestionAlert = false) }

    fun submit() {
        val state = uiState.value
        if (state.isLoading) return
        if (state.queryText.isBlank()) {
            optionsState.update { it.copy(showEmptyQueryAlert = true) }
            return
        }
        // Editing the box after a suggestion forfeits that suggestion's category. iOS settles
        // this *after* launching the lookup, so its first submit following an edit still saves
        // the card under the stale category (Suggest "Madame Bovary", retype "Krebs cycle", get
        // a Literature card). Resolving before the launch is the fix.
        val validSuggestion = state.validSuggestionMade && state.queryText == state.lastSuggestedAnswer
        optionsState.update {
            it.copy(
                isLoading = true,
                responseReceived = false,
                validSuggestionMade = validSuggestion,
                answerCategory = resolveAnswerCategory(validSuggestion, state.answerCategory, state.categories)
            )
        }
        viewModelScope.launch {
            when (val outcome = repository.analyzeClues(state.queryText.trim(), state.difficulties, state.categories)) {
                is AnalyzeOutcome.Success -> optionsState.update {
                    it.copy(
                        isLoading = false,
                        responseReceived = true,
                        responseText = outcome.responseText,
                        questionCount = outcome.questionCount,
                        modelUsed = outcome.modelUsed
                    )
                }
                AnalyzeOutcome.NoResults -> optionsState.update {
                    it.copy(
                        isLoading = false,
                        responseReceived = false,
                        responseText = "Your query didn't match any quizbowl questions in Qbreader. Please make sure that there aren't any typos or spelling errors in your query.",
                        questionCount = 0
                    )
                }
                is AnalyzeOutcome.Error -> optionsState.update {
                    it.copy(isLoading = false, responseReceived = false, responseText = outcome.message)
                }
            }
        }
    }

    private var freqListRequestId = 0

    fun fetchFrequencyList(category: Category) {
        val requestId = ++freqListRequestId
        optionsState.update { it.copy(showFreqList = true, freqListLoading = true, freqListItems = emptyList()) }
        viewModelScope.launch {
            val items = try {
                repository.fetchFrequencyList(category, uiState.value.difficulties)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (t: Throwable) {
                emptyList()
            }
            if (requestId == freqListRequestId) {
                optionsState.update { it.copy(freqListLoading = false, freqListItems = items) }
            }
        }
    }

    /**
     * Mirrors AnalyzeView.swift's `suggestTerm()`: walks shuffled candidate categories and fills
     * the query with a frequency-list answer not already covered by an existing card. Which of
     * the viable answers it picks is [SuggestionPicker]'s decision — top-5 by question count,
     * drawn at random, never the same answer twice in a row.
     *
     * The category that produced the suggestion is kept in [AnalyzeUiState.answerCategory] so a
     * card saved from the result opens pre-set to it.
     */
    fun suggestTerm() {
        optionsState.update { it.copy(awaitingSuggestion = true) }
        viewModelScope.launch {
            val state = uiState.value
            val candidateCategories = (if (state.categories.isEmpty()) Category.all else state.categories.toList()).shuffled()

            for (category in candidateCategories) {
                val items = try {
                    repository.fetchFrequencyList(category, state.difficulties)
                } catch (c: kotlinx.coroutines.CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    emptyList()
                }
                val suggestion = SuggestionPicker.pick(items, state.lastSuggestedAnswer) { answer ->
                    CardMatching.cardExists(answer, state.allCards)
                }
                if (suggestion != null) {
                    optionsState.update {
                        it.copy(
                            awaitingSuggestion = false,
                            queryText = suggestion.answer,
                            answerCategory = category,
                            validSuggestionMade = true,
                            lastSuggestedAnswer = suggestion.answer
                        )
                    }
                    return@launch
                }
            }

            optionsState.update { it.copy(awaitingSuggestion = false, showNoSuggestionAlert = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AnalyzeViewModel(appContainer().analyzeRepository, appContainer().cardRepository) }
        }
    }
}
