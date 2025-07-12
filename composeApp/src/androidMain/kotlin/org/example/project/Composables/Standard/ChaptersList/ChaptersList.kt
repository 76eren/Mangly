package org.example.project.Composables.Standard.ChaptersList

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import kotlinx.coroutines.withContext
import org.example.project.ViewModels.ExtensionMetadataViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers


@Composable
fun ChaptersList(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel
) {
    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value

    if (metadata == null) {
        Text(
            "Something went wrong while loading the chapters list.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    var chapters by remember { mutableStateOf<List<Source.ChapterValue>?>(null) }
    var image by remember { mutableStateOf<Source.ImageForChaptersList?>(null) }
    var summary by remember { mutableStateOf("") }
    var isSummaryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(targetUrl, metadata) {
        chapters = runCatching {
            withContext(Dispatchers.IO) {
                fetchChapterList(metadata.source, targetUrl)
            }
        }.getOrNull()

        image = runCatching {
            withContext(Dispatchers.IO) {
                fetchChapterImage(metadata.source, targetUrl)
            }
        }.getOrNull()

        summary = runCatching {
            withContext(Dispatchers.IO) {
                fetchSummary(metadata.source, targetUrl)
            }
        }.getOrDefault("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        image?.let {
            Image(
                painter = rememberAsyncImagePainter(it.imageUrl),
                contentDescription = "Comic Cover",
                contentScale = ContentScale.Crop,

                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(220.dp)
                    .height(320.dp)
                    .padding(bottom = 16.dp)
            )
        }

        if (summary.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Description",
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isSummaryExpanded) summary else summary.take(200) + "...",
                    modifier = Modifier
                        .clickable { isSummaryExpanded = !isSummaryExpanded }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isSummaryExpanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp)
                        .clickable { isSummaryExpanded = !isSummaryExpanded }
                )
            }
        }

        Text(
            text = "Chapters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        chapters?.let {
            if (it.isEmpty()) {
                Text("No chapters found.", style = MaterialTheme.typography.bodySmall)
            } else {
                it.forEach { chapter ->
                    OutlinedButton(
                        onClick = {
                            onChapterClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = chapter.title)
                    }
                }
            }
        } ?: Text("Loading chapters...", style = MaterialTheme.typography.bodySmall)
    }
}


suspend fun fetchChapterImage(source: Source, url: String): ImageForChaptersList {
    return source.getImageForChaptersList(url)
}

suspend fun fetchChapterList(source: Source, url: String): List<Source.ChapterValue> {
    return source.getChaptersFromChapterUrl(url)
}

suspend fun fetchSummary(source: Source, url: String): String {
    return source.getSummary(url)
}

fun onChapterClick() {

}