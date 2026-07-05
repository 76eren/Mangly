/*
 I do not know if I am going to regret this or not
 */

package com.eren76.mangly.composables.screens.home

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.composables.shared.dropdowns.DeleteDropdownMenu
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel

@Composable
fun ShowDownloadsInList(
    downloads: List<DownloadWithChapters>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    downloadsViewModel: DownloadsViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = downloads,
            key = { item -> item.download.downloadId }
        ) { item ->
            val title = item.download.mangaName ?: item.download.mangaUrl
            val downloadedChapters = item.chapters.count { it.isFullyDownloaded }
            val subtitle = "$downloadedChapters chapters downloaded"
            val canOpen = item.download.extensionId != null
            var isMenuExpanded by remember(item.download.downloadId) { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (canOpen) {
                                    onHomeMangaClick(
                                        mangaUrl = item.download.mangaUrl,
                                        extensionId = item.download.extensionId,
                                        extensionMetadataViewModel = extensionMetadataViewModel,
                                        navController = navHostController,
                                        isDownload = true
                                    )
                                }
                            },
                            onLongClick = { isMenuExpanded = true }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (!canOpen) {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    DeleteDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        text = "Delete all",
                        onDeleteClick = {
                            isMenuExpanded = false
                            downloadsViewModel.deleteDownload(item.download.downloadId, context)
                            Toast.makeText(context, "Download deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
