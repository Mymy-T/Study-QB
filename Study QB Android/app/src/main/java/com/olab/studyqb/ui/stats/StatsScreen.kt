package com.olab.studyqb.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onCardPerformanceClick: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
) {
    val totals by viewModel.lifetimeTotals.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Stats") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Lifetime", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("Total Points: ${totals.totalPoints ?: 0}")
                    Text("Correct: ${totals.totalCorrect ?: 0}")
                    Text("Incorrect: ${totals.totalIncorrect ?: 0}")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Card Performance") },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                androidx.compose.material3.TextButton(onClick = onCardPerformanceClick, modifier = Modifier.fillMaxWidth()) {
                    Text("View")
                }
            }

            Text("Games", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            if (games.isEmpty()) {
                Text("No buzz-in games played yet.")
            } else {
                LazyColumn {
                    items(games, key = { it.id }) { game ->
                        val dateLabel = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date.from(game.date))
                        Text("$dateLabel — ${game.points} pts — ${game.numberCorrect} correct, ${game.numberIncorrect} incorrect")
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
