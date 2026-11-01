package com.olab.studyqb.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olab.studyqb.domain.Category

/**
 * Multi-select dropdown over [Category], matching iOS's `CategorySelectionMenu`: defaults to
 * all categories selected, with Select All / Remove All quick actions. [icon] is the trigger
 * content — an icon, a text label, or both — and is sized to fit its content rather than
 * forced into a fixed circular icon-button bounds (which used to clip text labels).
 */
@Composable
fun CategoryMultiSelectMenu(
    selected: Set<Category>,
    onSelectedChange: (Set<Category>) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            icon()
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Category.all.forEach { category ->
            DropdownMenuItem(
                text = { Text(category.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = if (category in selected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = null
                    )
                },
                onClick = {
                    onSelectedChange(if (category in selected) selected - category else selected + category)
                }
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        DropdownMenuItem(text = { Text("Select All") }, onClick = { onSelectedChange(Category.all.toSet()) })
        DropdownMenuItem(text = { Text("Remove All") }, onClick = { onSelectedChange(emptySet()) })
    }
}
