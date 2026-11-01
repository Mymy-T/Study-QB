package com.olab.studyqb.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.ui.common.CardGrid
import com.olab.studyqb.ui.common.CardGridMode
import com.olab.studyqb.ui.common.CategoryMultiSelectMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onCardClick: (CardEntity) -> Unit,
    onAddClick: () -> Unit,
    viewModel: CardsViewModel = viewModel(factory = CardsViewModel.factory(editingMode = true))
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val cards by viewModel.visibleCards.collectAsStateWithLifecycle()
    val cardToDelete by viewModel.cardToDelete.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = { viewModel.toggleOnlyShowMarked() }) {
                            Icon(
                                imageVector = if (filter.onlyShowMarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Only marked for review",
                                tint = androidx.compose.ui.graphics.Color(0xFFFFCC00)
                            )
                        }
                        CategoryMultiSelectMenu(
                            selected = filter.categories,
                            onSelectedChange = viewModel::setCategories,
                            icon = { Icon(Icons.Filled.FilterList, contentDescription = "Filter by category") }
                        )
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.SortByAlpha, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortParameter.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        viewModel.setSortParameter(sort)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                        IconButton(onClick = onAddClick) {
                            Icon(Icons.Filled.Add, contentDescription = "Create card", tint = androidx.compose.ui.graphics.Color(0xFF34C759))
                        }
                    }
                )
                OutlinedTextField(
                    value = filter.searchText,
                    onValueChange = viewModel::setSearchText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text("Search for a card") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        }
    ) { padding ->
        CardGrid(
            cards = cards,
            mode = CardGridMode.EDITING,
            selectedIds = emptySet(),
            onTap = onCardClick,
            onLongPress = viewModel::requestDelete,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )

        if (cardToDelete.card != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                title = { Text("Delete Card?") },
                text = { Text("Are you sure you want to delete this card? This action is irreversible.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::confirmDelete) { Text("Yes") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::cancelDelete) { Text("No") }
                }
            )
        }
    }
}
