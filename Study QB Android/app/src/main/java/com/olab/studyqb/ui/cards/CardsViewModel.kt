package com.olab.studyqb.ui.cards

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
import kotlinx.coroutines.launch
import java.text.Normalizer

data class CardsFilterState(
    val searchText: String = "",
    val categories: Set<Category> = Category.all.toSet(),
    val onlyShowMarked: Boolean = false,
    val sortParameter: SortParameter = SortParameter.REVIEW_DATE_REVERSE
)

data class CardToDeleteState(val card: CardEntity? = null)

/**
 * Backs both the Cards tab (editing mode: tap to edit, long-press to delete, "+" to create)
 * and Review's manual card-selection flow (selection mode: tap to toggle membership in
 * [selectedIds], no delete/create) — matching iOS's single `ManageCardGrid` used both ways.
 */
class CardsViewModel(
    private val cardRepository: CardRepository,
    val editingMode: Boolean,
    initialSelectedIds: Set<Long> = emptySet()
) : ViewModel() {

    private val filterState = MutableStateFlow(CardsFilterState())
    val filter: StateFlow<CardsFilterState> = filterState

    private val selectedIdsState = MutableStateFlow(initialSelectedIds)
    val selectedIds: StateFlow<Set<Long>> = selectedIdsState

    private val cardToDeleteState = MutableStateFlow(CardToDeleteState())
    val cardToDelete: StateFlow<CardToDeleteState> = cardToDeleteState

    val visibleCards: StateFlow<List<CardEntity>> =
        combine(cardRepository.observeAll(), filterState) { cards, filter ->
            applyFilter(cards, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applyFilter(cards: List<CardEntity>, filter: CardsFilterState): List<CardEntity> {
        var pool = if (filter.onlyShowMarked) cards.filter { it.markedForReview } else cards
        pool = pool.filter { it.category in filter.categories }
        if (filter.searchText.isNotBlank()) {
            val needle = normalize(filter.searchText)
            pool = pool.filter { normalize(it.answer).contains(needle) }
        }
        return when (filter.sortParameter) {
            SortParameter.REVIEW_DATE_REVERSE -> pool.sortedByDescending { it.dateReviewed }
            SortParameter.REVIEW_DATE_FORWARD -> pool.sortedBy { it.dateReviewed }
            SortParameter.TITLE_ALPHABETICAL_FORWARD -> pool.sortedBy { it.answer.lowercase() }
            SortParameter.TITLE_ALPHABETICAL_REVERSE -> pool.sortedByDescending { it.answer.lowercase() }
        }
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()

    fun setSearchText(text: String) {
        filterState.value = filterState.value.copy(searchText = text)
    }

    fun setCategories(categories: Set<Category>) {
        filterState.value = filterState.value.copy(categories = categories)
    }

    fun toggleOnlyShowMarked() {
        filterState.value = filterState.value.copy(onlyShowMarked = !filterState.value.onlyShowMarked)
    }

    fun setSortParameter(sort: SortParameter) {
        filterState.value = filterState.value.copy(sortParameter = sort)
    }

    fun toggleSelection(card: CardEntity) {
        selectedIdsState.value =
            if (card.id in selectedIdsState.value) selectedIdsState.value - card.id
            else selectedIdsState.value + card.id
    }

    fun requestDelete(card: CardEntity) {
        cardToDeleteState.value = CardToDeleteState(card)
    }

    fun cancelDelete() {
        cardToDeleteState.value = CardToDeleteState()
    }

    fun confirmDelete() {
        val card = cardToDeleteState.value.card ?: return
        viewModelScope.launch {
            cardRepository.deleteCard(card)
            cardToDeleteState.value = CardToDeleteState()
        }
    }

    companion object {
        fun factory(editingMode: Boolean, initialSelectedIds: Set<Long> = emptySet()) = viewModelFactory {
            initializer {
                CardsViewModel(appContainer().cardRepository, editingMode, initialSelectedIds)
            }
        }
    }
}
