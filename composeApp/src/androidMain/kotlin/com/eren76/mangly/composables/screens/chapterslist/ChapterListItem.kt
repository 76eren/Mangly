package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.Source

@Composable
fun ChapterListItemCard(
    chapter: Source.ChapterValue,
    targetUrl: String,
    historyViewModel: HistoryViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    navHostController: NavHostController,
    scrollState: ScrollState,
    selectedChapterUrls: MutableList<String>,
    isSelectionMode: Boolean,
    isDownload: Boolean
) {
    val isRead =
        historyViewModel.historyWithChapters.value.any { history ->
            history.history.mangaUrl == targetUrl && history.readChapters.any { readChapter ->
                readChapter.chapterUrl == chapter.url
            }
        }

    val isSelected = selectedChapterUrls.contains(chapter.url)

    fun toggleSelectionForChapter() {
        if (isSelected) {
            selectedChapterUrls.remove(chapter.url)
        } else {
            selectedChapterUrls.add(chapter.url)
        }
    }

    fun handleNormalClick() {
        if (isSelectionMode) {
            toggleSelectionForChapter()
        } else {
            onChapterClick(
                navHostController,
                chapter.url,
                chaptersListViewModel,
                scrollState.value,
                chapter.title,
                targetUrl,
                isDownload
            )
        }
    }

    val sourceHost = remember(chapter.url) {
        runCatching {
            chapter.url.toUri().host.orEmpty().removePrefix("www.")
        }.getOrDefault("")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { handleNormalClick() },
                onLongClick = { toggleSelectionForChapter() }
            ),
        shape = RoundedCornerShape(10.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            isRead -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isSelected || isRead) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (sourceHost.isNotBlank()) {
                    Text(
                        text = sourceHost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected || isRead) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isSelected) "Selected" else "Read",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

