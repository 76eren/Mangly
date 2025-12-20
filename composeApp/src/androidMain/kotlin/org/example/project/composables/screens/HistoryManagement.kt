package org.example.project.composables.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.rooms.entities.HistoryChapterEntity
import org.example.project.rooms.entities.HistoryEntity
import org.example.project.rooms.entities.HistoryWithReadChapters
import org.example.project.viewmodels.ExtensionMetadataViewModel
import org.example.project.viewmodels.HistoryViewModel
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import java.util.SortedMap
import java.util.UUID

@Composable
fun HistoryManagement(
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    extensionMetaDataViewModel: ExtensionMetadataViewModel
) {
    val historyWithChapters by historyViewModel.historyWithChapters

    if (historyWithChapters.isEmpty()) {
        HistoryEmptyState()
        return
    }

    val historyData: SortedMap<Long, HistoryEntity> = remember(historyWithChapters) {
        getHistoryDataNewestToOldest(historyViewModel)
    }

    val sourcesById: Map<UUID, ExtensionMetadata> =
        remember(extensionMetaDataViewModel.getAllSources()) {
            extensionMetaDataViewModel.getAllSources().associateBy { metadata ->
                UUID.fromString(metadata.source.getExtensionId())
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = historyData.toList(),
                key = { (timeKey, historyEntity) ->
                    historyEntity.id.toString() + timeKey.toString()
                }
            ) { (timeKey, historyEntity) ->
                val sourceMetadata = sourcesById[historyEntity.extensionId]
                val source: Source? = sourceMetadata?.source

                HistoryRow(
                    historyEntity = historyEntity,
                    lastReadAt = timeKey,
                    source = source,
                    onClick = {
                        val encodedUrl = URLEncoder.encode(
                            historyEntity.mangaUrl,
                            Charsets.UTF_8.name()
                        )
                        extensionMetaDataViewModel.setSelectedSource(sourcesById[historyEntity.extensionId]!!) // TODO: not ideal
                        navHostController.navigate("chapters/$encodedUrl")
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start reading a manga to see it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(
    historyEntity: HistoryEntity,
    lastReadAt: Long,
    source: Source?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryCoverImage(
                mangaUrl = historyEntity.mangaUrl,
                source = source,
                modifier = Modifier
                    .size(64.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyEntity.mangaName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Last read: " + formatLastRead(lastReadAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryCoverImage(
    mangaUrl: String,
    source: Source?,
    modifier: Modifier = Modifier
) {
    if (source == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        return
    }

    HistoryCoverImageLoader(
        mangaUrl = mangaUrl,
        source = source,
        modifier = modifier
    )
}

@Composable
private fun HistoryCoverImageLoader(
    mangaUrl: String,
    source: Source,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var imageUrl by remember(source, mangaUrl) { mutableStateOf<String?>(null) }
    var headers by remember(source, mangaUrl) { mutableStateOf<List<Source.Header>>(emptyList()) }

    LaunchedEffect(source, mangaUrl) {
        val image: ImageForChaptersList? = runCatching {
            withContext(Dispatchers.IO) {
                source.getImageForChaptersList(mangaUrl)
            }
        }.getOrNull()

        imageUrl = image?.imageUrl
        headers = image?.headers ?: emptyList()
    }

    if (imageUrl.isNullOrEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val networkHeaders = NetworkHeaders.Builder().apply {
            headers.forEach { header ->
                this[header.name] = header.value
            }
        }.build()

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(networkHeaders)
            .crossfade(true)
            .build()

        AsyncImage(
            model = request,
            contentDescription = historyEntityDescriptionFromUrl(mangaUrl),
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun historyEntityDescriptionFromUrl(url: String): String = "Cover for $url"

fun getHistoryDataNewestToOldest(historyViewModel: HistoryViewModel): SortedMap<Long, HistoryEntity> {
    var historyEntitiesNewestToOldestReadState = sortedMapOf<Long, HistoryEntity>()
    val historyEntityByTime = HashMap<Long, HistoryEntity>()
    for (chapter: HistoryWithReadChapters in historyViewModel.historyWithChapters.value) {
        var latest = 0L
        for (readChapter: HistoryChapterEntity in chapter.readChapters) {
            val readAt = readChapter.readAt ?: continue
            if (readAt > latest) {
                latest = readAt
            }
        }
        if (latest > 0L) {
            historyEntityByTime[latest] = chapter.history
        }
    }

    val sortedKeys = historyEntityByTime.keys.sortedDescending()
    for (key in sortedKeys) {
        val historyEntity = historyEntityByTime[key] ?: continue
        historyEntitiesNewestToOldestReadState[key] = historyEntity
    }

    return historyEntitiesNewestToOldestReadState
}

fun formatLastRead(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestamp))
}
