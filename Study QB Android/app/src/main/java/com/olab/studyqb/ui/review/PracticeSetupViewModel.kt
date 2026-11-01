package com.olab.studyqb.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.domain.Category
import com.olab.studyqb.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CardSelectionMethod { AUTOMATIC, RANDOM, MANUAL }
enum class PracticeStyle { CLASSIC, BUZZ_IN }

data class ResolvedSession(
    val cardIds: List<Long>,
    val sequentialClues: Boolean,
    val singleClueMode: Boolean,
    val spacedRepetition: Boolean
)

data class PracticeSetupUiState(
    val allCards: List<CardEntity> = emptyList(),
    val dueTodayCount: Int = 0,
    val selectionMethod: CardSelectionMethod = CardSelectionMethod.AUTOMATIC,
    val randomCountText: String = "10",
    val randomCategories: Set<Category> = Category.all.toSet(),
    val randomOnlyMarked: Boolean = false,
    val manualSelectedIds: Set<Long> = emptySet(),
    val practiceStyle: PracticeStyle = PracticeStyle.CLASSIC,
    val sequentialClues: Boolean = false,
    val singleClueMode: Boolean = false,
    val showInsufficientCardsAlert: Boolean = false,
    val showMissingNotesAlert: Boolean = false,
    val missingNotesAnswers: List<String> = emptyList(),
    val navigateToClassic: ResolvedSession? = null,
    val navigateToBuzzIn: ResolvedSession? = null
) {
    val availableRandomPool: List<CardEntity>
        get() = allCards.filter { it.category in randomCategories && (!randomOnlyMarked || it.markedForReview) }

    val randomCount: Int
        get() = randomCountText.toIntOrNull()?.coerceIn(1, maxOf(1, availableRandomPool.size)) ?: 1

    val manualSelectionSummary: String
        get() {
            val answers = allCards.filter { it.id in manualSelectedIds }.map { it.answer }.take(5)
            return if (answers.isEmpty()) "" else answers.joinToString(", ")
        }
}

class PracticeSetupViewModel(private val cardRepository: CardRepository) : ViewModel() {

    private val optionsState = MutableStateFlow(PracticeSetupUiState())

    val uiState: StateFlow<PracticeSetupUiState> =
        combine(cardRepository.observeAll(), optionsState) { cards, options ->
            options.copy(allCards = cards)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PracticeSetupUiState())

    fun refreshDueCount() {
        viewModelScope.launch {
            val due = cardRepository.getDueToday()
            optionsState.update { it.copy(dueTodayCount = due.size) }
        }
    }

    fun setSelectionMethod(method: CardSelectionMethod) = optionsState.update {
        if (method == CardSelectionMethod.AUTOMATIC) it.copy(selectionMethod = method, singleClueMode = false)
        else it.copy(selectionMethod = method)
    }
    fun setRandomCountText(text: String) = optionsState.update { it.copy(randomCountText = text.filter(Char::isDigit)) }
    fun setRandomCategories(categories: Set<Category>) = optionsState.update { it.copy(randomCategories = categories) }
    fun toggleRandomOnlyMarked() = optionsState.update { it.copy(randomOnlyMarked = !it.randomOnlyMarked) }
    fun setManualSelectedIds(ids: Set<Long>) = optionsState.update { it.copy(manualSelectedIds = ids) }

    fun setPracticeStyle(style: PracticeStyle) = optionsState.update {
        if (style == PracticeStyle.BUZZ_IN) it.copy(practiceStyle = style, sequentialClues = false, singleClueMode = false)
        else it.copy(practiceStyle = style)
    }

    fun setSequentialClues(value: Boolean) = optionsState.update {
        if (value) it.copy(sequentialClues = true, singleClueMode = false) else it.copy(sequentialClues = false)
    }

    fun setSingleClueMode(value: Boolean) = optionsState.update {
        if (value) it.copy(singleClueMode = true, sequentialClues = false) else it.copy(singleClueMode = false)
    }

    fun dismissInsufficientCardsAlert() = optionsState.update { it.copy(showInsufficientCardsAlert = false) }
    fun dismissMissingNotesAlert() = optionsState.update { it.copy(showMissingNotesAlert = false) }
    fun consumeNavigation() = optionsState.update { it.copy(navigateToClassic = null, navigateToBuzzIn = null) }

    fun startPractice() {
        val state = uiState.value
        val resolvedCards: List<CardEntity> = when (state.selectionMethod) {
            CardSelectionMethod.AUTOMATIC -> state.allCards.filter(::isDue).shuffled()
            CardSelectionMethod.RANDOM -> state.availableRandomPool.shuffled().take(state.randomCount)
            CardSelectionMethod.MANUAL -> state.allCards.filter { it.id in state.manualSelectedIds }.shuffled()
        }

        if (resolvedCards.isEmpty()) {
            optionsState.update { it.copy(showInsufficientCardsAlert = true) }
            return
        }

        val blankNotesAnswers = resolvedCards.filter { it.personalNotes.isBlank() }.map { it.answer }
        if (blankNotesAnswers.isNotEmpty()) {
            optionsState.update { it.copy(showMissingNotesAlert = true, missingNotesAnswers = blankNotesAnswers) }
            return
        }

        val session = ResolvedSession(
            cardIds = resolvedCards.map { it.id },
            sequentialClues = state.sequentialClues,
            singleClueMode = state.singleClueMode,
            spacedRepetition = state.selectionMethod == CardSelectionMethod.AUTOMATIC
        )
        optionsState.update {
            if (state.practiceStyle == PracticeStyle.CLASSIC) it.copy(navigateToClassic = session)
            else it.copy(navigateToBuzzIn = session)
        }
    }

    private fun isDue(card: CardEntity): Boolean {
        val startOfTomorrow = java.time.LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        return card.fsrs.due.isBefore(startOfTomorrow)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PracticeSetupViewModel(appContainer().cardRepository) }
        }
    }
}
