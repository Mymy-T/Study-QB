package com.olab.studyqb.ui.review.buzzin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.data.fsrs.Rating
import com.olab.studyqb.ui.review.ResolvedSession
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuzzInScreen(
    session: ResolvedSession,
    onDone: () -> Unit,
    viewModel: BuzzInViewModel = viewModel(factory = BuzzInViewModel.factory(session))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(state.sessionSaved) {
        if (state.sessionSaved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card ${state.currentIndex + 1} of ${state.cards.size} — ${state.sessionPoints} pts") },
                navigationIcon = {
                    IconButton(onClick = onDone, enabled = !state.savingSession) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleMarkedForReview() }) {
                        Icon(
                            imageVector = if (state.currentCard?.markedForReview == true) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Mark for review",
                            tint = Color(0xFFFFCC00)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.sessionSaved) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }

        if (state.sessionComplete) {
            SessionSummary(
                cardsReviewed = state.cards.size,
                points = state.sessionPoints,
                correct = state.numberCorrect,
                incorrect = state.numberIncorrect,
                savingSession = state.savingSession,
                onDiscard = { viewModel.discardSession() },
                onSave = { viewModel.saveSession() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        if (!state.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF007AFF).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                if (state.phase == BuzzPhase.LOADING_TOSSUP) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Generating tossup…", modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    val visibleWords = if (state.phase == BuzzPhase.ANSWER_REVEAL) state.words else state.words.take(state.revealedWordCount)
                    Text(visibleWords.joinToString(" "), modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            }

            if (state.usedFallback) {
                Text("Offline — using simplified format", color = Color.Gray)
            }

            when (state.phase) {
                BuzzPhase.REVEALING -> {
                    LinearProgressIndicator(progress = { state.questionProgress }, modifier = Modifier.fillMaxWidth())
                }
                BuzzPhase.COUNTDOWN -> {
                    Text(
                        "${state.countdownSecondsRemaining}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = if (state.countdownSecondsRemaining <= 2) Color.Red else Color.Unspecified,
                        style = androidx.compose.material3.MaterialTheme.typography.displayMedium
                    )
                    LinearProgressIndicator(
                        progress = { state.countdownTicks / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (state.countdownSecondsRemaining <= 2) Color.Red else Color(0xFFFF9500)
                    )
                    OutlinedTextField(
                        value = state.typedGuess,
                        onValueChange = viewModel::setTypedGuess,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your answer") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { viewModel.submitEarly() })
                    )
                }
                BuzzPhase.ANSWER_REVEAL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF34C759).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(state.currentCard?.answer.orEmpty())
                    }
                }
                BuzzPhase.LOADING_TOSSUP -> {}
            }

            when {
                state.phase == BuzzPhase.LOADING_TOSSUP -> {}
                state.phase == BuzzPhase.REVEALING -> {
                    Button(onClick = { viewModel.buzz() }, enabled = state.revealedWordCount > 0, modifier = Modifier.fillMaxWidth()) {
                        Text("BUZZ!")
                    }
                }
                state.phase == BuzzPhase.COUNTDOWN -> {
                    Button(onClick = { viewModel.submitEarly() }, modifier = Modifier.fillMaxWidth()) { Text("Submit") }
                }
                state.phase == BuzzPhase.ANSWER_REVEAL && state.gotIt == null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.judge(true) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                        ) { Text("Got it ✓") }
                        Button(
                            onClick = { viewModel.judge(false) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                        ) { Text("Didn't get it") }
                    }
                }
                state.phase == BuzzPhase.ANSWER_REVEAL && state.gotIt != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Button(onClick = { viewModel.nextCard() }, modifier = Modifier.fillMaxWidth()) { Text("Next Card") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummary(
    cardsReviewed: Int,
    points: Int,
    correct: Int,
    incorrect: Int,
    savingSession: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Session Complete!", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text("Cards reviewed: $cardsReviewed")
        Text("Points: $points")
        Text("Correct: $correct — Incorrect: $incorrect")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDiscard) { Text("Discard") }
            Button(onClick = onSave, enabled = !savingSession) { Text("Save") }
        }
    }
}
