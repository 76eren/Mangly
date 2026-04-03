package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun ChaptersHeaderSection(
    targetUrl: String,
    image: ImageForChaptersList?,
    mangaName: String,
    summary: String,
    isSummaryExpanded: Boolean,
    extensionName: String,
    isFavorite: Boolean,
    onToggleSummary: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isCoverPreviewVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            image?.let {
                ChaptersCoverImage(
                    targetUrl = targetUrl,
                    image = it,
                    onClick = { isCoverPreviewVisible = true },
                    modifier = Modifier
                        .width(112.dp)
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = mangaName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = extensionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }
        }

        if (summary.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (isSummaryExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onToggleSummary)
                    )
                    Text(
                        text = if (isSummaryExpanded) "Show less" else "Read more",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onToggleSummary)
                    )
                }
            }
        }

        if (isCoverPreviewVisible) {
            image?.let {
                ChaptersCoverPreviewDialog(
                    targetUrl = targetUrl,
                    image = it,
                    onDismiss = { isCoverPreviewVisible = false }
                )
            }
        }
    }
}

@Composable
private fun ChaptersCoverImage(
    targetUrl: String,
    image: ImageForChaptersList,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageRequest = buildCoverImageRequest(targetUrl = targetUrl, image = image)

    ElevatedCard(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures(
                onTap = { onClick?.invoke() }
            )
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

@Composable
private fun ChaptersCoverPreviewDialog(
    targetUrl: String,
    image: ImageForChaptersList,
    onDismiss: () -> Unit
) {
    val imageRequest = buildCoverImageRequest(targetUrl = targetUrl, image = image)

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
