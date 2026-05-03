package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// Dialog preview for chapter cover with network image and headers
@Composable
fun ChaptersCoverPreviewDialog(
    targetUrl: String,
    image: ImageForChaptersList,
    onDismiss: () -> Unit
) {
    val imageRequest = buildCoverPreviewImageRequest(targetUrl = targetUrl, image = image)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Comic Cover Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
                loading = { ImageLoadingComposable() },
                error = { ImageLoadingErrorComposable() }
            )
        }
    }
}

// Dialog preview for chapter cover with local downloaded file no networking
@Composable
fun ChaptersCoverPreviewDialog(
    coverFile: File,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            SubcomposeAsyncImage(
                model = coverFile,
                contentDescription = "Comic Cover Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
                loading = { ImageLoadingComposable() },
                error = { ImageLoadingErrorComposable() }
            )
        }
    }
}

@Composable
private fun buildCoverPreviewImageRequest(
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

    val cacheKey = "chapters_cover_preview_${targetUrl.hashCode()}"
    return ImageRequest.Builder(LocalContext.current)
        .data(image.imageUrl)
        .httpHeaders(networkHeaders)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .crossfade(true)
        .build()
}

