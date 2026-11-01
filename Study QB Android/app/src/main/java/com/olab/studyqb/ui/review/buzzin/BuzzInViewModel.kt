package com.olab.studyqb.ui.review.buzzin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.fsrs.FsrsScheduler
import com.olab.studyqb.data.fsrs.Rating
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.data.repository.StatsRepository
import com.olab.studyqb.data.repository.TossupRepository
import com.olab.studyqb.domain.FuzzyMatch
import com.olab.studyqb.ui.appContainer
import com.olab.studyqb.ui.review.ResolvedSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

enum class BuzzPhase { LOADING_TOSSUP, REVEALING, COUNTDOWN, ANSWER_REVEAL }

private const val TICK_MS = 50L
private const val TICKS_PER_WORD = 4 // 200ms/word at ~300 WPM
private const val COUNTDOWN_TICKS = 100 // 5000ms / 50ms

data class BuzzInUiState(
    val loaded: Boolean = false,
    val cards: List<CardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val phase: BuzzPhase = BuzzPhase.LOADING_TOSSUP,
    val words: List<String> = emptyList(),
    val revealedWordCount: Int = 0,
    val usedFallback: Boolean = false,
    val countdownTicks: Int = 0,
    val typedGuess: String = "",
    val gotIt: Boolean? = null,
    val ratingChosen: Rating? = null,
    val sessionPoints: Int = 0,
    val numberCorrect: Int = 0,
    val numberIncorrect: Int = 0,
    val sessionComplete: Boolean = false,
    val spacedRepetition: Boolean = false,
    val sessionSaved: Boolean = false,
    val savingSession: Boolean = false
) {
    val currentCard: CardEntity? get() = cards.getOrNull(currentIndex)
    val questionProgress: Float get() = if (words.isEmpty()) 0f else revealedWordCount.toFloat() / words.size
    val countdownSecondsRemaining: Int get() = kotlin.math.ceil((COUNTDOWN_TICKS - countdownTicks) * TICK_MS / 1000.0).toInt()
    val suggestedGotIt: Boolean?
        get() {
            val card = currentCard ?: return null
            if (phase != BuzzPhase.ANSWER_REVEAL || typedGuess.isBlank()) return null
            return FuzzyMatch.isMatch(typedGuess, card.answer)
        }
    val projectedDueDate: Instant?
        get() {
            val card = currentCard ?: return null
            val rating = ratingChosen ?: return null
            return FsrsScheduler.scheduleReview(card.fsrs.toFsrsCard(), rating, Instant.now()).due
        }
}

class BuzzInViewModel(
    private val cardRepository: CardRepository,
    private val tossupRepository: TossupRepository,
    private val statsRepository: StatsRepository,
    private val session: ResolvedSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuzzInUiState(spacedRepetition = session.spacedRepetition))
    val uiState: StateFlow<BuzzInUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            val byId = cardRepository.getByIds(session.cardIds).associateBy { it.id }
            val cards = session.cardIds.mapNotNull { byId[it] }
            _uiState.update { it.copy(loaded = true, cards = cards) }
            if (cards.isNotEmpty()) loadCurrentCardTossup()
        }
    }

    private fun loadCurrentCardTossup() {
        val card = _uiState.value.currentCard ?: return
        _uiState.update {
            it.copy(
                phase = BuzzPhase.LOADING_TOSSUP,
                words = emptyList(),
                revealedWordCount = 0,
                usedFallback = false,
                countdownTicks = 0,
                typedGuess = "",
                gotIt = null,
                ratingChosen = null
            )
        }
        viewModelScope.launch {
            val result = tossupRepository.getOrGenerateTossup(card)
            val words = result.text.split(Regex("\\s+")).filter { it.isNotBlank() }
            _uiState.update { it.copy(phase = BuzzPhase.REVEALING, words = words, usedFallback = result.usedFallback) }
            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            var wordTickCounter = 0
            while (isActive) {
                delay(TICK_MS)
                val state = _uiState.value
                when (state.phase) {
                    BuzzPhase.REVEALING -> {
                        wordTickCounter++
                        if (wordTickCounter >= TICKS_PER_WORD) {
                            wordTickCounter = 0
                            if (state.revealedWordCount < state.words.size) {
                                _uiState.update { it.copy(revealedWordCount = it.revealedWordCount + 1) }
                            }
                            if (_uiState.value.revealedWordCount >= _uiState.value.words.size) {
                                _uiState.update { it.copy(phase = BuzzPhase.COUNTDOWN, countdownTicks = 0) }
                            }
                        }
                    }
                    BuzzPhase.COUNTDOWN -> {
                        _uiState.update { it.copy(countdownTicks = it.countdownTicks + 1) }
                        if (_uiState.value.countdownTicks >= COUNTDOWN_TICKS) {
                            _uiState.update { it.copy(phase = BuzzPhase.ANSWER_REVEAL) }
                            return@launch
                        }
                    }
                    else -> return@launch
                }
            }
        }
    }

    fun buzz() {
        val state = _uiState.value
        if (state.phase != BuzzPhase.REVEALING || state.revealedWordCount == 0) return
        _uiState.update { it.copy(phase = BuzzPhase.COUNTDOWN, countdownTicks = 0) }
    }

    fun setTypedGuess(text: String) = _uiState.update { it.copy(typedGuess = text) }

    fun submitEarly() {
        if (_uiState.value.phase != BuzzPhase.COUNTDOWN) return
        tickerJob?.cancel()
        _uiState.update { it.copy(phase = BuzzPhase.ANSWER_REVEAL) }
    }

    fun toggleMarkedForReview() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val updatedCard = card.copy(markedForReview = !card.markedForReview)
        _uiState.update { st ->
            st.copy(cards = st.cards.toMutableList().also { it[st.currentIndex] = updatedCard })
        }
        viewModelScope.launch { cardRepository.toggleMarkedForReview(card, updatedCard.markedForReview) }
    }

    fun judge(correct: Boolean) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        if (state.phase != BuzzPhase.ANSWER_REVEAL || state.gotIt != null) return

        val buzzPositionPercent = if (state.words.isEmpty()) 100.0 else state.revealedWordCount.toDouble() / state.words.size * 100.0
        val interrupted = state.revealedWordCount < state.words.size
        val pointsDelta = if (correct) 10 else if (interrupted) -5 else 0
        val ratingChosen = if (correct) {
            if (interrupted) Rating.GOOD else Rating.HARD
        } else {
            Rating.AGAIN
        }

        _uiState.update {
            it.copy(
                gotIt = correct,
                ratingChosen = ratingChosen,
                sessionPoints = it.sessionPoints + pointsDelta,
                numberCorrect = it.numberCorrect + if (correct) 1 else 0,
                numberIncorrect = it.numberIncorrect + if (!correct) 1 else 0
            )
        }

        viewModelScope.launch {
            statsRepository.recordCardAttempt(card.answer, card.category, correct, buzzPositionPercent)
        }
    }

    fun setRating(rating: Rating) = _uiState.update { it.copy(ratingChosen = rating) }

    fun nextCard() {
        val state = _uiState.value
        val card = state.currentCard
        val rating = state.ratingChosen
        if (state.spacedRepetition && card != null && rating != null) {
            viewModelScope.launch {
                val updated = FsrsScheduler.scheduleReview(card.fsrs.toFsrsCard(), rating, Instant.now())
                cardRepository.updateFsrsState(card, updated)
            }
        }

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.cards.size) {
            tickerJob?.cancel()
            _uiState.update { it.copy(sessionComplete = true) }
        } else {
            _uiState.update { it.copy(currentIndex = nextIndex) }
            loadCurrentCardTossup()
        }
    }

    fun saveSession() {
        val state = _uiState.value
        if (state.savingSession || state.sessionSaved) return
        _uiState.update { it.copy(savingSession = true) }
        viewModelScope.launch {
            try {
                statsRepository.saveGame(state.sessionPoints, state.numberCorrect, state.numberIncorrect)
                _uiState.update { it.copy(sessionSaved = true) }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _uiState.update { it.copy(savingSession = false) }
            }
        }
    }

    fun discardSession() = _uiState.update { it.copy(sessionSaved = true) }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }

    companion object {
        fun factory(session: ResolvedSession) = viewModelFactory {
            initializer {
                val container = appContainer()
                BuzzInViewModel(container.cardRepository, container.tossupRepository, container.statsRepository, session)
            }
        }
    }
}
