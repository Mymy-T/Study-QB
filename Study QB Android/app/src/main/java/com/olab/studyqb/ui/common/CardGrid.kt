package com.olab.studyqb.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.domain.ColorPalette
import java.text.DateFormat

enum class CardGridMode { EDITING, SELECTION }

/**
 * The reusable 2-column card grid, used both by the Cards tab (editing mode: tap-to-edit,
 * long-press-to-delete) and by Review's manual card-selection flow (selection mode:
 * tap-to-toggle, blue border when selected), matching iOS's `ManageCardGrid`.
 */
@Composable
fun CardGrid(
    cards: List<CardEntity>,
    mode: CardGridMode,
    selectedIds: Set<Long>,
    onTap: (CardEntity) -> Unit,
    onLongPress: (CardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards match the selected criteria.")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cards, key = { it.id }) { card ->
            CardTile(
                card = card,
                selected = mode == CardGridMode.SELECTION && card.id in selectedIds,
                onTap = { onTap(card) },
                onLongPress = { if (mode == CardGridMode.EDITING) onLongPress(card) }
            )
        }
    }
}

@Composable
private fun CardTile(
    card: CardEntity,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val color = ColorPalette.rgbaToColor(ColorPalette.stringToRgba(card.rgba))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = if (selected) BorderStroke(4.dp, Color(0xFF007AFF).copy(alpha = 0.6f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = card.answer,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                color = androidx.compose.ui.graphics.Color.Black
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reviewed on", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Black)
                Text(
                    text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(java.util.Date.from(card.dateReviewed)),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }
            Text(card.category.displayName, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Black)
        }
    }
}
