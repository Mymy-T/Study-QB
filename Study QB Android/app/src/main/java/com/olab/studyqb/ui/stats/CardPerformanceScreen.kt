package com.olab.studyqb.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.domain.ColorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPerformanceScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
) {
    val cardStats by viewModel.cardStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Performance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (cardStats.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No cards reviewed yet.")
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(cardStats, key = { it.id }) { stat ->
                Column(Modifier.padding(16.dp, 8.dp)) {
                    Text(stat.answer, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    Text(stat.category.displayName, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val buzzScore = stat.averageBuzzPositionPercent / 100
                        Text("Seen ${stat.timesSeen}x — ${stat.timesCorrect} correct — avg buzz")
                        Text(
                            String.format("%.2f", buzzScore),
                            color = ColorPalette.colorForNormalizedScore(buzzScore),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
