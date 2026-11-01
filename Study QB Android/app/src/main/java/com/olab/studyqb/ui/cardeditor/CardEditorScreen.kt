package com.olab.studyqb.ui.cardeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.ColorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    cardId: Long?,
    prefillAnswer: String? = null,
    prefillDetails: String? = null,
    prefillCategory: Category? = null,
    onDone: () -> Unit,
    viewModel: CardEditorViewModel = viewModel(
        factory = CardEditorViewModel.factory(cardId, prefillAnswer, prefillDetails, prefillCategory)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isDirty() && !state.isNewCard) "${state.navigationTitle} (unsaved changes)" else state.navigationTitle) },
                navigationIcon = {
                    IconButton(onClick = onDone, enabled = !state.saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }, enabled = !state.saving) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color(0xFF34C759))
                    }
                }
            )
        }
    ) { padding ->
        if (state.saved || !state.loaded) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ColorPalette.rgbaToColor(state.rgba))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionLabel("Answer Line")
            OutlinedTextField(
                value = state.answer,
                onValueChange = viewModel::setAnswer,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter the answerline of the question") }
            )

            SectionLabel("Category")
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) }
                )
                ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                    Category.all.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.setCategory(category)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            SectionLabel("Select Color")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorPalette.colors.forEach { paletteColor ->
                    val swatch = paletteColor.copy(alpha = ColorPalette.DEFAULT_ALPHA)
                    val isSelected = ColorPalette.rgbaToColor(state.rgba) == swatch
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .then(
                                if (isSelected) Modifier.border(3.dp, Color(0xFF007AFF), CircleShape) else Modifier
                            )
                            .clickable { viewModel.setColor(ColorPalette.colorToRgba(swatch)) }
                    )
                }
            }

            SectionLabel("Personal Notes")
            Text(
                "List the facts that you want to review. List easier clues closer to the top and " +
                    "place clues on separate lines to make use of the clue reveal feature when reviewing.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.toggleMarkedForReview() }
            ) {
                Icon(
                    imageVector = if (state.markedForReview) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = Color(0xFFFFCC00)
                )
                Text(" Marked For Review")
            }
            OutlinedTextField(
                value = state.personalNotes,
                onValueChange = viewModel::setPersonalNotes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = { Text("Add any personal notes here.") }
            )

            SectionLabel("Details")
            OutlinedTextField(
                value = state.details,
                onValueChange = viewModel::setDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                placeholder = { Text("Clue summary details (supports **bold**)") }
            )

            SectionLabel("Last Reviewed")
            Button(onClick = { viewModel.markReviewedNow() }) {
                Text("I Reviewed It!")
            }
            Text("This card was last reviewed on ${state.dateReviewedLabel}.")

            SectionLabel("Scheduled Review Date (Spaced Repetition)")
            Text("This card is scheduled to be reviewed on ${state.dueDateLabel}")

            HorizontalDivider()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
}
