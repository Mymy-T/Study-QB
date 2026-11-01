package com.olab.studyqb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olab.studyqb.data.prefs.NotificationSettings
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.data.repository.SettingsRepository
import com.olab.studyqb.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showDeleteAllAlert: Boolean = false,
    val showClearCacheAlert: Boolean = false,
    val showNotificationsDeniedAlert: Boolean = false,
    val deleteAllConfirmationText: String = ""
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationsEnabled(enabled)
    }

    fun onNotificationPermissionDenied() = _uiState.update { it.copy(showNotificationsDeniedAlert = true) }
    fun dismissNotificationsDeniedAlert() = _uiState.update { it.copy(showNotificationsDeniedAlert = false) }

    fun setReminderMode(mode: Int) = viewModelScope.launch {
        settingsRepository.setReminderMode(mode)
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        settingsRepository.setReminderTime(hour, minute)
    }

    fun setWeekdayEnabled(weekday: Int, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setWeekdayEnabled(weekday, enabled)
    }

    fun setReminderIntervalHours(hours: Int) = viewModelScope.launch {
        settingsRepository.setReminderIntervalHours(hours)
    }

    fun requestDeleteAllCards() = _uiState.update { it.copy(showDeleteAllAlert = true) }
    fun cancelDeleteAllCards() = _uiState.update { it.copy(showDeleteAllAlert = false, deleteAllConfirmationText = "") }
    fun setDeleteAllConfirmationText(text: String) = _uiState.update { it.copy(deleteAllConfirmationText = text) }
    fun confirmDeleteAllCards() = viewModelScope.launch {
        cardRepository.deleteAll()
        _uiState.update { it.copy(showDeleteAllAlert = false, deleteAllConfirmationText = "") }
    }

    fun requestClearCache() = _uiState.update { it.copy(showClearCacheAlert = true) }
    fun cancelClearCache() = _uiState.update { it.copy(showClearCacheAlert = false) }
    fun confirmClearCache() = viewModelScope.launch {
        cardRepository.clearAllCachedTossups()
        _uiState.update { it.copy(showClearCacheAlert = false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer().settingsRepository, appContainer().cardRepository) }
        }
    }
}
