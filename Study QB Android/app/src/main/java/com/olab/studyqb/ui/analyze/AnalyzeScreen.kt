package com.olab.studyqb.ui.analyze

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.PendingCardPrefill
import com.olab.studyqb.domain.Category
import com.olab.studyqb.ui.common.CategoryMultiSelectMenu
import com.olab.studyqb.ui.common.DifficultyMultiSelectMenu
import com.olab.studyqb.ui.common.HelpSheet
import com.olab.studyqb.ui.common.MarkdownBoldText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    onCardSaved: () -> Unit,
    viewModel: AnalyzeViewModel = viewModel(factory = AnalyzeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var freqMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyze") },
                actions = {
                    IconButton(onClick = { viewModel.setShowHelp(true) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help")
                    }
                    IconButton(onClick = { freqMenuExpanded = true }) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Freq Lists")
                    }
                    DropdownMenu(expanded = freqMenuExpanded, onDismissRequest = { freqMenuExpanded = false }) {
                        Category.all.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    freqMenuExpanded = false
                                    viewModel.fetchFrequencyList(category)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryMultiSelectMenu(
                    selected = state.categories,
                    onSelectedChange = viewModel::setCategories,
                    icon = {
                        val label = if (state.categories.isEmpty() || state.categories.size == Category.all.size) {
                            "Categories: All"
                        } else {
                            "Categories: ${state.categories.size}"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                DifficultyMultiSelectMenu(
                    selected = state.difficulties,
                    onSelectedChange = viewModel::setDifficulties,
                    icon = {
                        val label = if (state.difficulties.isEmpty() || state.difficulties.size == com.olab.studyqb.domain.Difficulty.all.size) {
                            "Difficulties: All"
                        } else {
                            "Difficulties: ${state.difficulties.size}"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.queryText,
                    onValueChange = viewModel::setQueryText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter query") },
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { viewModel.submit() })
                )
                IconButton(onClick = { viewModel.suggestTerm() }, enabled = !state.awaitingSuggestion) {
                    if (state.awaitingSuggestion) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Suggest a term")
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.isLoading) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("This may take a moment...", modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        state.responseText?.let { MarkdownBoldText(text = it, selectable = true) }
                    }
                }
            }

            if (state.responseReceived) {
                Text(
                    "${state.questionCount} quizbowl questions were analyzed using the ${state.modelUsed} model.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        PendingCardPrefill.set(state.queryText, state.responseText.orEmpty(), state.answerCategory)
                        onCardSaved()
                    },
                    enabled = state.responseReceived
                ) { Text("Save") }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Button(onClick = { viewModel.submit() }, enabled = !state.isLoading) { Text("Find Common Clues") }
            }
        }
    }

    if (state.showHelp) {
        HelpSheet(
            title = "Usage Details (1.2)",
            paragraphs = listOf(
                "In the 'Analyze' tab, you can input a keyword or answerline, and the app will try to find common clues in quizbowl questions with the entered answerline. The quizbowl questions are obtained from the QBReader website at www.qbreader.org. You can select certain categories to narrow your search, as well as the difficulties that best suit your needs. Please make sure that the keyword or answerline you input exactly matches the desired answerline (don't have typos or spelling errors). After a response is generated, you can save it to a card for later review.",
                "In the 'Manage Cards' tab, you can view all of your saved and created cards. You can click on a card to view its details and any personal notes you have left for yourself when reviewing. Long press on a card to delete it.",
                "In the 'Review Cards' tab, you will be able to review the cards you have saved. You can customize your reviewing session. For example, you can select specific cards to review."
            ),
            onDismiss = { viewModel.setShowHelp(false) }
        )
    }

    if (state.showFreqList) {
        FrequencyListSheet(
            loading = state.freqListLoading,
            items = state.freqListItems,
            cards = state.allCards,
            onDismiss = { viewModel.dismissFreqList() }
        )
    }

    if (state.showEmptyQueryAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissEmptyQueryAlert() },
            title = { Text("Empty Query") },
            text = { Text("Please enter an answerline or keyword to search for.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissEmptyQueryAlert() }) { Text("OK") }
            }
        )
    }

    if (state.showNoSuggestionAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissNoSuggestionAlert() },
            title = { Text("No Suggestions Found") },
            text = { Text("You already have cards for the top terms in the selected categories.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissNoSuggestionAlert() }) { Text("Ok") }
            }
        )
    }
}
