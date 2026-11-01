package com.olab.studyqb.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.olab.studyqb.ui.common.CardGrid
import com.olab.studyqb.ui.common.CardGridMode
import com.olab.studyqb.ui.common.CategoryMultiSelectMenu

/** The manual card-selection flow reached from Practice Setup's "Edit Selection" button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSelectionScreen(
    initialSelectedIds: Set<Long>,
    onDone: (Set<Long>) -> Unit,
    viewModel: CardsViewModel = viewModel(factory = CardsViewModel.factory(editingMode = false, initialSelectedIds = initialSelectedIds))
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val cards by viewModel.visibleCards.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { onDone(selectedIds) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done")
                        }
                    },
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
            mode = CardGridMode.SELECTION,
            selectedIds = selectedIds,
            onTap = viewModel::toggleSelection,
            onLongPress = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
