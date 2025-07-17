package org.example.project.Composables.Standard.Read

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.Composables.Standard.Read.Styles.ComicReader
import org.example.project.ViewModels.ExtensionMetadataViewModel

// Reader composable that handles the display of chapter images
@Composable
fun Read(targetUrl: String, extensionMetadataViewModel: ExtensionMetadataViewModel) {
    var chapterImages by remember { mutableStateOf<Source.ChapterImages?>(null) }

    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value
    if (metadata == null) {
        Text(
            text = "Something went wrong while loading the chapter images.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LaunchedEffect(targetUrl, metadata) {
        chapterImages = withContext(Dispatchers.IO) {
            getChapterImages(targetUrl, metadata)
        }
    }

    if (chapterImages != null && chapterImages!!.images.isNotEmpty()) {
        // Todo: styles should be selectable dynamically
        ComicReader(
            images = chapterImages!!.images,
            headers = chapterImages!!.headers,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Text(
            text = "Loading images or no images available...",
            modifier = Modifier.padding(16.dp)
        )
    }
}





// Existing function to fetch chapter images
suspend fun getChapterImages(url: String, metadata: ExtensionMetadata): Source.ChapterImages {
    try {
        val data = metadata.source.getChapterImages(url)
        return data
    } catch (e: Exception) {
        return Source.ChapterImages(emptyList(), emptyList())
    }
}