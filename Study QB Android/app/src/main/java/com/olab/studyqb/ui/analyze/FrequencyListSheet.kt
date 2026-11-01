package com.olab.studyqb.ui.analyze

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.network.qbreader.dto.FrequencyEntryDto
import com.olab.studyqb.domain.CardMatching

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyListSheet(
    loading: Boolean,
    items: List<FrequencyEntryDto>,
    cards: List<CardEntity>,
    onDismiss: () -> Unit
) {
    var hideExisting by remember { mutableStateOf(true) }
    val displayedItems = if (hideExisting) items.filter { !CardMatching.cardExists(it.answer, cards) } else items

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = hideExisting, onCheckedChange = { hideExisting = it })
                Text(" Hide cards I already have")
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Answer", fontWeight = FontWeight.Bold)
                Text("Count", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            if (loading) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("This may take a moment...", modifier = Modifier.padding(top = 8.dp))
                }
            } else if (displayedItems.isEmpty()) {
                Text("No results found.", modifier = Modifier.padding(vertical = 16.dp))
            }

            LazyColumn(Modifier.height(360.dp)) {
                items(displayedItems.withIndex().toList()) { (index, item) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}  ${item.answer}")
                        Text("${item.count}")
                    }
                    HorizontalDivider()
                }
            }

            Text(
                "Only questions with the selected difficulties (from the previous view) are shown. " +
                    "You can find more lists and options at https://www.qbreader.org/db/frequency-list/",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
