package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.manglyextension.plugins.Source
import com.eren76.manglyextension.plugins.Source.ImageForChaptersList
import java.io.File

// Chapter cover with network image and headers
@Composable
fun ChapterCover(
    targetUrl: String,
    image: ImageForChaptersList,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageRequest = buildCoverImageRequest(targetUrl = targetUrl, image = image)

    ElevatedCard(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick?.invoke() })
        },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Comic Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            loading = { ImageLoadingComposable() },
            error = { ImageLoadingErrorComposable() }
        )
    }
}

// Chapter cover with local downloaded file no networking
@Composable
fun ChapterCover(
    coverFile: File,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick?.invoke() })
        },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        SubcomposeAsyncImage(
            model = coverFile,
            contentDescription = "Comic Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            loading = { ImageLoadingComposable() },
            error = { ImageLoadingErrorComposable() }
        )
    }
}

@Composable
private fun buildCoverImageRequest(
    targetUrl: String,
    image: ImageForChaptersList
): ImageRequest {
    val headers: List<Source.Header> = image.headers.map { header ->
        Source.Header(header.name, header.value)
    }

    val networkHeaders = NetworkHeaders.Builder().apply {
        for (header in headers) {
            this[header.name] = header.value
        }
    }.build()

    val cacheKey = "chapters_cover_${targetUrl.hashCode()}"
    return ImageRequest.Builder(LocalContext.current)
        .data(image.imageUrl)
        .httpHeaders(networkHeaders)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .crossfade(true)
        .build()
}

