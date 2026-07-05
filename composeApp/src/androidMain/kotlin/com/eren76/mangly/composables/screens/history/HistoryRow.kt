package com.eren76.mangly.composables.screens.history

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.dropdowns.DeleteDropdownMenu
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.Source

@Composable
internal fun HistoryRow(
    historyEntity: HistoryEntity,
    lastReadAt: Long,
    source: Source?,
    historyViewModel: HistoryViewModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isDeleteDropdownMenuExpanded by remember(historyEntity.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { isDeleteDropdownMenuExpanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistoryCoverImage(
                    historyEntity = historyEntity,
                    source = source,
                    historyViewModel = historyViewModel,
                    modifier = Modifier
                        .width(110.dp)
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = historyEntity.mangaName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Last read: " + formatLastRead(lastReadAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DeleteDropdownMenu(
                    expanded = isDeleteDropdownMenuExpanded,
                    onDismissRequest = { isDeleteDropdownMenuExpanded = false },
                    text = "Delete history",
                    onDeleteClick = {
                        isDeleteDropdownMenuExpanded = false
                        historyViewModel.deleteWholeHistoryByHistoryEntity(historyEntity, context)
                        Toast.makeText(context, "History deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

