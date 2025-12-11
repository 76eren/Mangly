package org.example.project.Composables.Standard.Read

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.Composables.Standard.Read.viewer.ReaderMode
import org.example.project.Composables.Standard.Read.viewer.webtoon.WebtoonReaderMode
import org.example.project.ViewModels.ExtensionMetadataViewModel

@Composable
fun Read(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel

) {
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

        // Todo: this does not work properly yet
        PreloadImages(chapterImages!!.images, chapterImages!!.headers)


        // Todo: styles should be selectable dynamically
        val webtoonReaderMode: ReaderMode = WebtoonReaderMode

        webtoonReaderMode.Content(
            images = chapterImages!!.images,
            headers = chapterImages!!.headers,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
        )

    } else {
        Text(
            text = "Loading images or no images available...",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun PreloadImages(
    urls: List<String>,
    headers: List<Source.Header>
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val imageLoader = coil3.ImageLoader(context)
        for (url in urls) {
            val networkHeaders = NetworkHeaders.Builder().apply {
                for (header in headers) {
                    this[header.name] = header.value
                }
            }.build()

            val request = ImageRequest.Builder(context)
                .data(url)
                .httpHeaders(networkHeaders)
                .build()

            imageLoader.enqueue(request)
        }
    }
}


suspend fun getChapterImages(url: String, metadata: ExtensionMetadata): Source.ChapterImages {
    try {
        val data = metadata.source.getChapterImages(url)
        return data
    } catch (e: Exception) {
        return Source.ChapterImages(emptyList(), emptyList())
    }
}