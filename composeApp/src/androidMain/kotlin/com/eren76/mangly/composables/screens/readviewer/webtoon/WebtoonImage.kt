package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.content.Context
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable

/**
 * Cache to store measured heights of images by their URL.
 * This helps maintain scroll position when images are recycled in LazyColumn.
 */
object ImageHeightCache {
    private val heightCache = mutableStateMapOf<String, Dp>()

    fun getHeight(imageUrl: String): Dp? = heightCache[imageUrl]

    fun setHeight(imageUrl: String, height: Dp) {
        heightCache[imageUrl] = height
    }

    fun clear() {
        heightCache.clear()
    }
}

@Composable
fun WebtoonImage(
    imageUrl: String,
    networkHeaders: NetworkHeaders,
    context: Context,
    index: Int,
    totalImages: Int
) {
    val density = LocalDensity.current
    val cachedHeight = remember(imageUrl) { ImageHeightCache.getHeight(imageUrl) }

    val imageRequest = remember(imageUrl, networkHeaders) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(networkHeaders)
            .crossfade(false)
            .build()
    }

    val modifier = if (cachedHeight != null) {
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = cachedHeight)
    } else {
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp)
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Page ${index + 1} of $totalImages",
        modifier = modifier.onGloballyPositioned { coordinates ->
            val heightDp = with(density) { coordinates.size.height.toDp() }
            if (heightDp > 0.dp) {
                ImageHeightCache.setHeight(imageUrl, heightDp)
            }
        },
        contentScale = ContentScale.FillWidth,
        loading = {
            ImageLoadingComposable(
                index = index,
                minHeight = cachedHeight ?: 200.dp
            )
        },
        error = {
            ImageLoadingErrorComposable(index = index)
        }
    )
}



