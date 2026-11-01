package com.olab.studyqb.ui.review.classic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.fsrs.FsrsScheduler
import com.olab.studyqb.data.fsrs.Rating
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.ui.appContainer
import com.olab.studyqb.ui.review.ResolvedSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** One reviewable unit: either a whole card, or (in single-clue mode) a single clue line from a card. */
data class ReviewUnit(val parentCard: CardEntity, val clueText: String)

data class ClassicPracticeUiState(
    val loaded: Boolean = false,
    val units: List<ReviewUnit> = emptyList(),
    val currentIndex: Int = 0,
    val answerRevealed: Boolean = false,
    val revealedLineCount: Int = 1,
    val ratingChosen: Rating? = null,
    val finished: Boolean = false,
    val showDetails: Boolean = false,
    val sequentialClues: Boolean = false,
    val spacedRepetition: Boolean = false
) {
    val currentUnit: ReviewUnit? get() = units.getOrNull(currentIndex)

    /**
     * Reversed to match PracticeView.swift, which calls `clues.reverse()` immediately after
     * splitting `personalNotes` — in both `onAppear` and the Next Card handler. Cards are
     * written easiest-clue-first, so a gradual reveal has to run bottom-up to unfold the way a
     * real tossup does: obscure clue first, giveaway last.
     *
     * Only `questionText` depends on the order; every other consumer uses `.size`, and
     * non-sequential mode bypasses this entirely.
     */
    val clueLines: List<String>
        get() = currentUnit?.clueText?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.reversed()
            ?: emptyList()
    val questionText: String get() = if (sequentialClues) clueLines.take(revealedLineCount).joinToString("\n") else currentUnit?.clueText.orEmpty()
    val allCluesRevealed: Boolean get() = !sequentialClues || revealedLineCount >= clueLines.size
    val projectedDueDate: Instant?
        get() {
            val unit = currentUnit ?: return null
            val rating = ratingChosen ?: return null
            return FsrsScheduler.scheduleReview(unit.parentCard.fsrs.toFsrsCard(), rating, Instant.now()).due
        }
}

class ClassicPracticeViewModel(
    private val cardRepository: CardRepository,
    private val session: ResolvedSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ClassicPracticeUiState(sequentialClues = session.sequentialClues, spacedRepetition = session.spacedRepetition)
    )
    val uiState: StateFlow<ClassicPracticeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cards = cardRepository.getByIds(session.cardIds)
                .associateBy { it.id }
                .let { byId -> session.cardIds.mapNotNull { byId[it] } } // preserve the already-shuffled order

            val units = if (session.singleClueMode) {
                cards.flatMap { card ->
                    card.personalNotes.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.map { line -> ReviewUnit(card, line) }
                }.shuffled()
            } else {
                cards.map { ReviewUnit(it, it.personalNotes) }
            }

            _uiState.update { it.copy(loaded = true, units = units, revealedLineCount = 1) }
        }
    }

    fun revealAnswer() {
        val unit = _uiState.value.currentUnit ?: return
        val nowRevealed = !_uiState.value.answerRevealed
        _uiState.update { it.copy(answerRevealed = nowRevealed) }
        if (nowRevealed) viewModelScope.launch { cardRepository.markReviewedNow(unit.parentCard) }
    }

    fun nextClue() {
        _uiState.update {
            if (it.sequentialClues && it.revealedLineCount < it.clueLines.size) it.copy(revealedLineCount = it.revealedLineCount + 1) else it
        }
    }

    fun toggleMarkedForReview() {
        val state = _uiState.value
        val unit = state.currentUnit ?: return
        val updatedCard = unit.parentCard.copy(markedForReview = !unit.parentCard.markedForReview)
        _uiState.update { st ->
            st.copy(units = st.units.map { if (it.parentCard.id == updatedCard.id) it.copy(parentCard = updatedCard) else it })
        }
        viewModelScope.launch { cardRepository.toggleMarkedForReview(unit.parentCard, updatedCard.markedForReview) }
    }

    fun setRating(rating: Rating) = _uiState.update { it.copy(ratingChosen = rating) }

    fun setShowDetails(show: Boolean) = _uiState.update { it.copy(showDetails = show) }

    fun nextCard() {
        val state = _uiState.value
        val unit = state.currentUnit ?: return
        viewModelScope.launch {
            if (state.spacedRepetition && state.ratingChosen != null) {
                val updated = FsrsScheduler.scheduleReview(unit.parentCard.fsrs.toFsrsCard(), state.ratingChosen, Instant.now())
                cardRepository.updateFsrsState(unit.parentCard, updated)
            }
            val nextIndex = state.currentIndex + 1
            val wrapped = nextIndex >= state.units.size
            _uiState.update {
                it.copy(
                    currentIndex = if (wrapped) 0 else nextIndex,
                    answerRevealed = false,
                    revealedLineCount = 1,
                    ratingChosen = null,
                    finished = wrapped && (state.spacedRepetition)
                )
            }
        }
    }

    companion object {
        fun factory(session: ResolvedSession) = viewModelFactory {
            initializer { ClassicPracticeViewModel(appContainer().cardRepository, session) }
        }
    }
}
