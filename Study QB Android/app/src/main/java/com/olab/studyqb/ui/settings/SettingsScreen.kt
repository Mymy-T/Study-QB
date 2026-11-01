package com.olab.studyqb.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.data.prefs.ReminderMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setNotificationsEnabled(true) else viewModel.onNotificationPermissionDenied()
    }

    fun handleToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setNotificationsEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotificationsEnabled(true)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Notifications", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = settings.notificationsEnabled, onCheckedChange = { handleToggle(it) })
                Text(" Study Reminders")
            }

            if (settings.notificationsEnabled) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = settings.reminderMode == ReminderMode.DAILY,
                        onClick = { viewModel.setReminderMode(ReminderMode.DAILY) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Daily") }
                    SegmentedButton(
                        selected = settings.reminderMode == ReminderMode.INTERVAL,
                        onClick = { viewModel.setReminderMode(ReminderMode.INTERVAL) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Every N Hours") }
                }

                if (settings.reminderMode == ReminderMode.DAILY) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = settings.reminderHour,
                            onValueChange = { viewModel.setReminderTime(it, settings.reminderMinute) },
                            range = 0..23,
                            label = "Hour (0-23)",
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = settings.reminderMinute,
                            onValueChange = { viewModel.setReminderTime(settings.reminderHour, it) },
                            range = 0..59,
                            label = "Minute (0-59)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val labels = listOf("S", "M", "T", "W", "T", "F", "S")
                        for (weekday in 1..7) {
                            val enabled = (settings.selectedWeekdaysMask and (1 shl (weekday - 1))) != 0
                            FilterChip(
                                selected = enabled,
                                onClick = { viewModel.setWeekdayEnabled(weekday, !enabled) },
                                label = { Text(labels[weekday - 1]) }
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.setReminderIntervalHours((settings.reminderIntervalHours - 1).coerceIn(1, 12)) }) { Text("-") }
                        Text("Every ${settings.reminderIntervalHours} hour${if (settings.reminderIntervalHours == 1) "" else "s"}")
                        IconButton(onClick = { viewModel.setReminderIntervalHours((settings.reminderIntervalHours + 1).coerceIn(1, 12)) }) { Text("+") }
                    }
                }
            }

            HorizontalDivider()

            Text("Delete", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            TextButton(onClick = { viewModel.requestClearCache() }) { Text("Delete Cached Tossup Data") }
            TextButton(onClick = { viewModel.requestDeleteAllCards() }) { Text("DELETE ALL CARDS") }
        }
    }

    if (uiState.showDeleteAllAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteAllCards() },
            title = { Text("ARE YOU SURE?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete ALL of your cards? This action is irreversible. Type \"delete\" to confirm.")
                    OutlinedTextField(
                        value = uiState.deleteAllConfirmationText,
                        onValueChange = { viewModel.setDeleteAllConfirmationText(it) },
                        placeholder = { Text("Type \"delete\" to confirm") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteAllCards() },
                    enabled = uiState.deleteAllConfirmationText.trim().lowercase() == "delete"
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { viewModel.cancelDeleteAllCards() }) { Text("Cancel") } }
        )
    }
    if (uiState.showClearCacheAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearCache() },
            title = { Text("Delete cached tossup data?") },
            confirmButton = { TextButton(onClick = { viewModel.confirmClearCache() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelClearCache() }) { Text("Cancel") } }
        )
    }
    if (uiState.showNotificationsDeniedAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNotificationsDeniedAlert() },
            title = { Text("Notifications Disabled") },
            text = { Text("Study QB needs notification permission to send study reminders. You can enable this in the Settings app.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissNotificationsDeniedAlert()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissNotificationsDeniedAlert() }) { Text("Cancel") } }
        )
    }
}

/**
 * Buffers its own text locally so backspacing to empty (or typing an out-of-range/non-numeric
 * value) doesn't get overwritten by the last-committed [value] on the next recomposition — only
 * commits [onValueChange] once the typed text parses to an in-[range] Int.
 */
@Composable
private fun NumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            new.toIntOrNull()?.let { parsed -> if (parsed in range) onValueChange(parsed) }
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
