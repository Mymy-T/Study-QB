package com.olab.studyqb.ui.cardeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.ColorPalette
import com.olab.studyqb.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.util.Date

data class CardEditorUiState(
    val isNewCard: Boolean = true,
    val loaded: Boolean = false,
    val answer: String = "",
    val details: String = "",
    val category: Category = Category.LITERATURE,
    val personalNotes: String = "",
    val markedForReview: Boolean = false,
    val rgba: List<Double> = ColorPalette.colorToRgba(ColorPalette.colorForCategory(Category.LITERATURE)),
    val dateReviewed: Instant = Instant.now(),
    val fsrsDueDate: Instant? = null,
    val saved: Boolean = false,
    val saving: Boolean = false,
    // Internal bookkeeping for the category -> color "settle guard" (see setCategory doc).
    val originalLoadedCategory: Category = Category.LITERATURE,
    val firstCategoryChangeConsumed: Boolean = false
) {
    val dateReviewedLabel: String get() = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date.from(dateReviewed))
    val dueDateLabel: String get() = fsrsDueDate?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(it)) } ?: "—"
    val navigationTitle: String get() = if (isNewCard) "Create Card" else "Edit Card"
}

class CardEditorViewModel(
    private val cardRepository: CardRepository,
    private val cardId: Long?,
    prefillAnswer: String?,
    prefillDetails: String?,
    prefillCategory: Category?
) : ViewModel() {

    private var originalEntity: CardEntity? = null
    private var dirtyBaseline: CardEditorUiState? = null

    /**
     * [prefillCategory] arrives from Analyze, which knows the category behind a suggested answer
     * (or behind a filter narrowed to one). It is applied here, in the *initial* state, rather
     * than through [setCategory]: the initial state is what `dirtyBaseline` snapshots, so a
     * brand-new card would otherwise open already claiming unsaved changes.
     *
     * [CardEditorUiState.rgba] is set alongside it because the swatch row tests exact color
     * equality to mark its selection — leaving the default red here would show a Science card
     * with a green swatch and nothing highlighted.
     */
    private val _uiState = MutableStateFlow(
        (prefillCategory ?: Category.LITERATURE).let { initialCategory ->
            CardEditorUiState(
                isNewCard = cardId == null,
                answer = prefillAnswer.orEmpty(),
                details = prefillDetails.orEmpty(),
                category = initialCategory,
                rgba = ColorPalette.colorToRgba(ColorPalette.colorForCategory(initialCategory))
            )
        }
    )
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    init {
        if (cardId != null) {
            viewModelScope.launch {
                val entity = cardRepository.getById(cardId) ?: return@launch
                originalEntity = entity
                val loaded = CardEditorUiState(
                    isNewCard = false,
                    loaded = true,
                    answer = entity.answer,
                    details = entity.details,
                    category = entity.category,
                    personalNotes = entity.personalNotes,
                    markedForReview = entity.markedForReview,
                    rgba = ColorPalette.stringToRgba(entity.rgba),
                    dateReviewed = entity.dateReviewed,
                    fsrsDueDate = entity.fsrs.due,
                    originalLoadedCategory = entity.category,
                    firstCategoryChangeConsumed = false
                )
                _uiState.value = loaded
                dirtyBaseline = loaded
            }
        } else {
            _uiState.update { it.copy(loaded = true) }
            dirtyBaseline = _uiState.value
        }
    }

    fun isDirty(): Boolean {
        val baseline = dirtyBaseline ?: return false
        val current = _uiState.value
        return baseline.answer != current.answer ||
            baseline.details != current.details ||
            baseline.category != current.category ||
            baseline.personalNotes != current.personalNotes ||
            baseline.markedForReview != current.markedForReview ||
            baseline.rgba != current.rgba ||
            baseline.dateReviewed != current.dateReviewed
    }

    fun setAnswer(value: String) = _uiState.update { it.copy(answer = value) }
    fun setDetails(value: String) = _uiState.update { it.copy(details = value) }
    fun setPersonalNotes(value: String) = _uiState.update { it.copy(personalNotes = value) }
    fun toggleMarkedForReview() = _uiState.update { it.copy(markedForReview = !it.markedForReview) }
    fun markReviewedNow() = _uiState.update { it.copy(dateReviewed = Instant.now()) }

    /** Directly picking a color swatch always sets it, independent of the category settle-guard. */
    fun setColor(rgba: List<Double>) = _uiState.update { it.copy(rgba = rgba) }

    /**
     * Matches CardEditorView.swift's `updateColorUponCategoryChange`: a brand-new card always
     * follows category -> color. An existing card suppresses the *first* category change from
     * recoloring (to preserve a manually-picked color that doesn't match the default palette),
     * UNLESS the card's originally-loaded category was Literature (SwiftUI's onChange never
     * fired at all during load in that case, so the guard was never "primed" to suppress
     * anything, and the first genuine user change behaves like any other change).
     */
    fun setCategory(newCategory: Category) {
        _uiState.update { state ->
            val shouldRecolor = when {
                state.isNewCard -> true
                !state.firstCategoryChangeConsumed -> state.originalLoadedCategory == Category.LITERATURE
                else -> true
            }
            state.copy(
                category = newCategory,
                rgba = if (shouldRecolor) ColorPalette.colorToRgba(ColorPalette.colorForCategory(newCategory)) else state.rgba,
                firstCategoryChangeConsumed = true
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.saving || state.saved) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val existing = originalEntity
                if (existing != null) {
                    cardRepository.updateCard(
                        existing.copy(
                            answer = state.answer,
                            details = state.details,
                            category = state.category,
                            personalNotes = state.personalNotes,
                            markedForReview = state.markedForReview,
                            rgba = ColorPalette.rgbaToString(state.rgba),
                            dateReviewed = state.dateReviewed
                        )
                    )
                } else {
                    cardRepository.createCard(
                        answer = state.answer,
                        details = state.details,
                        category = state.category,
                        personalNotes = state.personalNotes,
                        rgba = state.rgba,
                        markedForReview = state.markedForReview,
                        dateReviewed = state.dateReviewed
                    )
                }
                _uiState.update { it.copy(saved = true) }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    companion object {
        fun factory(
            cardId: Long?,
            prefillAnswer: String? = null,
            prefillDetails: String? = null,
            prefillCategory: Category? = null
        ) = viewModelFactory {
            initializer {
                CardEditorViewModel(
                    appContainer().cardRepository,
                    cardId,
                    prefillAnswer,
                    prefillDetails,
                    prefillCategory
                )
            }
        }
    }
}
