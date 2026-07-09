package com.eren76.mangly.composables.shared.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.downloads.models.DownloadQueueStatus

@Composable
fun DownloadQueuePanel(
    queueItems: List<DownloadQueueItem>,
    onCancelQueue: () -> Unit,
    onDismissQueueItem: (DownloadQueueItem) -> Unit = {},
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    if (queueItems.isEmpty()) return

    var isExpanded: Boolean by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val activeCount: Int = queueItems.count { it.isActive }
    val failedCount: Int = queueItems.count { it.status == DownloadQueueStatus.Failed }
    val cancelledCount: Int = queueItems.count { it.status == DownloadQueueStatus.Cancelled }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            QueueHeader(
                activeCount = activeCount,
                failedCount = failedCount,
                cancelledCount = cancelledCount,
                isExpanded = isExpanded,
                onToggleExpanded = { isExpanded = !isExpanded },
                onCancelQueue = onCancelQueue
            )

            if (activeCount > 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (isExpanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = EXPANDED_QUEUE_MAX_HEIGHT),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = queueItems,
                        key = { it.workId }
                    ) { item: DownloadQueueItem ->
                        DownloadQueueRow(
                            item = item,
                            onDismiss = if (item.isActive) {
                                null
                            } else {
                                { onDismissQueueItem(item) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    activeCount: Int,
    failedCount: Int,
    cancelledCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCancelQueue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "Download queue",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = queueSummary(activeCount, failedCount, cancelledCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (activeCount > 0) {
            IconButton(onClick = onCancelQueue) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel download queue"
                )
            }
        }

        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = if (isExpanded) {
                    "Collapse download queue"
                } else {
                    "Expand download queue"
                }
            )
        }
    }
}

@Composable
private fun DownloadQueueRow(
    item: DownloadQueueItem,
    onDismiss: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.chapterName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.status.label,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor(item.status),
                maxLines = 1
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss download queue item"
                    )
                }
            }
        }

        Text(
            text = "${item.mangaName} - ${item.positionLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor(item.status),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun statusColor(status: DownloadQueueStatus) = when (status) {
    DownloadQueueStatus.Failed -> MaterialTheme.colorScheme.error
    DownloadQueueStatus.Cancelled -> MaterialTheme.colorScheme.outline
    DownloadQueueStatus.Done -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun queueSummary(
    activeCount: Int,
    failedCount: Int,
    cancelledCount: Int
): String {
    return buildList {
        if (activeCount > 0) add("$activeCount active")
        if (failedCount > 0) add("$failedCount failed")
        if (cancelledCount > 0) add("$cancelledCount cancelled")
        if (isEmpty()) add("No active downloads")
    }.joinToString(" - ")
}

private val EXPANDED_QUEUE_MAX_HEIGHT = 200.dp
