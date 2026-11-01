package com.olab.studyqb.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.ui.common.CategoryMultiSelectMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSetupScreen(
    onEditManualSelection: (Set<Long>) -> Unit,
    onStartClassic: (ResolvedSession) -> Unit,
    onStartBuzzIn: (ResolvedSession) -> Unit,
    pendingManualSelectionResult: LongArray? = null,
    viewModel: PracticeSetupViewModel = viewModel(factory = PracticeSetupViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDueCount()
    }

    LaunchedEffect(pendingManualSelectionResult) {
        pendingManualSelectionResult?.let { viewModel.setManualSelectedIds(it.toSet()) }
    }

    LaunchedEffect(state.navigateToClassic) {
        state.navigateToClassic?.let { onStartClassic(it); viewModel.consumeNavigation() }
    }
    LaunchedEffect(state.navigateToBuzzIn) {
        state.navigateToBuzzIn?.let { onStartBuzzIn(it); viewModel.consumeNavigation() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Review") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Card Selection", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CardSelectionMethod.entries.forEachIndexed { index, method ->
                    SegmentedButton(
                        selected = state.selectionMethod == method,
                        onClick = { viewModel.setSelectionMethod(method) },
                        shape = SegmentedButtonDefaults.itemShape(index, CardSelectionMethod.entries.size)
                    ) { Text(method.name.lowercase().replaceFirstChar { it.uppercase() }) }
                }
            }

            when (state.selectionMethod) {
                CardSelectionMethod.AUTOMATIC -> {
                    Text("${state.dueTodayCount} card(s) are due today.")
                }
                CardSelectionMethod.RANDOM -> {
                    OutlinedTextField(
                        value = state.randomCountText,
                        onValueChange = viewModel::setRandomCountText,
                        label = { Text("Number of cards") }
                    )
                    Text("Will use ${state.randomCount} card(s).", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    CategoryMultiSelectMenu(
                        selected = state.randomCategories,
                        onSelectedChange = viewModel::setRandomCategories,
                        icon = {
                            val label = if (state.randomCategories.size == com.olab.studyqb.domain.Category.all.size) {
                                "Categories: All"
                            } else {
                                "Categories: ${state.randomCategories.size} selected"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.randomOnlyMarked, onCheckedChange = { viewModel.toggleRandomOnlyMarked() })
                        Text(" Only review-marked cards")
                    }
                    Text("${state.availableRandomPool.size} card(s) available.")
                }
                CardSelectionMethod.MANUAL -> {
                    Text("${state.manualSelectedIds.size} card(s) selected.")
                    if (state.manualSelectionSummary.isNotEmpty()) Text(state.manualSelectionSummary)
                    Button(onClick = { onEditManualSelection(state.manualSelectedIds) }) {
                        Text("Edit Selection")
                    }
                }
            }

            Text("Practice Options", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PracticeStyle.entries.forEachIndexed { index, style ->
                    SegmentedButton(
                        selected = state.practiceStyle == style,
                        onClick = { viewModel.setPracticeStyle(style) },
                        shape = SegmentedButtonDefaults.itemShape(index, PracticeStyle.entries.size)
                    ) { Text(if (style == PracticeStyle.CLASSIC) "Classic" else "Buzz In") }
                }
            }

            if (state.practiceStyle == PracticeStyle.CLASSIC) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = state.sequentialClues, onCheckedChange = { viewModel.setSequentialClues(it) })
                    Text(" Slowly reveal clues?")
                }
                if (state.selectionMethod != CardSelectionMethod.AUTOMATIC) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.singleClueMode, onCheckedChange = { viewModel.setSingleClueMode(it) })
                        Text(" Only 1 clue at a time?")
                    }
                }
            } else {
                Text("Read along as the tossup is revealed and buzz in when you know the answer!")
            }

            Button(onClick = { viewModel.startPractice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Start Practice")
            }

            if (state.showInsufficientCardsAlert) {
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Not Enough Cards", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text("There aren't enough cards available for this selection method.")
                        Button(onClick = { viewModel.dismissInsufficientCardsAlert() }) { Text("OK") }
                    }
                }
            }
            if (state.showMissingNotesAlert) {
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Missing Personal Notes", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text("These cards have no personal notes to review: ${state.missingNotesAnswers.joinToString(", ")}")
                        Button(onClick = { viewModel.dismissMissingNotesAlert() }) { Text("OK") }
                    }
                }
            }
        }
    }
}
