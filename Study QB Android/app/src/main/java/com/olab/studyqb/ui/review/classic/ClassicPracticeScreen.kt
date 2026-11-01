package com.olab.studyqb.ui.review.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.data.fsrs.Rating
import com.olab.studyqb.ui.common.MarkdownBoldText
import com.olab.studyqb.ui.review.ResolvedSession
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicPracticeScreen(
    session: ResolvedSession,
    onDone: () -> Unit,
    viewModel: ClassicPracticeViewModel = viewModel(factory = ClassicPracticeViewModel.factory(session))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card ${state.currentIndex + 1} of ${state.units.size}") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleMarkedForReview() }) {
                        Icon(
                            imageVector = if (state.currentUnit?.parentCard?.markedForReview == true) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Mark for review",
                            tint = Color(0xFFFFCC00)
                        )
                    }
                    IconButton(onClick = { viewModel.setShowDetails(true) }) {
                        Icon(Icons.Filled.Info, contentDescription = "Details")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.loaded || state.units.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }
        if (state.finished) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Congrats! You have finished all of your automatically scheduled cards for today.")
                    Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
                }
            }
            return@Scaffold
        }

        HapticOnChange(state.revealedLineCount, HapticFeedbackType.SegmentFrequentTick)
        HapticOnChange(state.answerRevealed, HapticFeedbackType.Confirm)
        HapticOnChange(state.ratingChosen, HapticFeedbackType.SegmentTick)
        HapticOnChange(state.currentIndex, HapticFeedbackType.GestureEnd)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.sequentialClues) {
                LinearProgressIndicator(
                    progress = { if (state.clueLines.isEmpty()) 1f else state.revealedLineCount.toFloat() / state.clueLines.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${state.revealedLineCount} of ${state.clueLines.size} clues revealed")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF007AFF).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.revealAnswer() }
                    .pointerInput(state.currentIndex) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 40) viewModel.nextClue()
                        }
                    }
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                MarkdownBoldText(text = state.questionText)
            }

            if (state.answerRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF34C759).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(state.currentUnit?.parentCard?.answer.orEmpty())
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.revealAnswer() }) {
                    Text(if (state.answerRevealed) "Hide Answer" else "Show Answer")
                }
                if (state.sequentialClues) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Button(onClick = { viewModel.nextClue() }, enabled = state.revealedLineCount < state.clueLines.size) {
                        Text("Next Clue")
                    }
                }
            }

            if (state.spacedRepetition) {
                state.projectedDueDate?.let {
                    Text("Would be due: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(it))}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Rating.AGAIN to "Again", Rating.HARD to "Hard", Rating.GOOD to "Good", Rating.EASY to "Easy").forEach { (rating, label) ->
                        Button(
                            onClick = { viewModel.setRating(rating) },
                            colors = if (state.ratingChosen == rating) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                        ) { Text(label) }
                    }
                }
            }

            Button(
                onClick = { viewModel.nextCard() },
                enabled = !state.spacedRepetition || state.ratingChosen != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Next Card") }
        }

        if (state.showDetails) {
            ModalBottomSheet(onDismissRequest = { viewModel.setShowDetails(false) }) {
                Column(Modifier.padding(16.dp)) {
                    MarkdownBoldText(text = state.currentUnit?.parentCard?.details.orEmpty(), selectable = true)
                }
            }
        }
    }
}

/** Mirrors PracticeView.swift's `.sensoryFeedback(trigger:)`: buzzes when [value] changes, skipping the initial composition. */
@Composable
private fun <T> HapticOnChange(value: T, type: HapticFeedbackType) {
    val haptic = LocalHapticFeedback.current
    var isFirst by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (isFirst) {
            isFirst = false
        } else {
            haptic.performHapticFeedback(type)
        }
    }
}
