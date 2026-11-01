package com.olab.studyqb.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.db.dao.LifetimeTotals
import com.olab.studyqb.data.db.entities.BuzzGameStatEntity
import com.olab.studyqb.data.db.entities.CardStatEntity
import com.olab.studyqb.data.repository.StatsRepository
import com.olab.studyqb.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(repository: StatsRepository) : ViewModel() {

    val lifetimeTotals: StateFlow<LifetimeTotals> = repository.observeLifetimeTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LifetimeTotals(0, 0, 0))

    val games: StateFlow<List<BuzzGameStatEntity>> = repository.observeGamesNewestFirst()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cardStats: StateFlow<List<CardStatEntity>> = repository.observeCardStatsSortedByAnswer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { StatsViewModel(appContainer().statsRepository) }
        }
    }
}
