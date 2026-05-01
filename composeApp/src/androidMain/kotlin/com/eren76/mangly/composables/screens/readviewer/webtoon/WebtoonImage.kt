package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
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
    page: ReaderPage,
    index: Int,
    totalImages: Int,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val density = LocalDensity.current
    val cachedHeight = remember(page.url) { ImageHeightCache.getHeight(page.url) }

    var imageBitmap: ImageBitmap? = null
    val state = page.state
    if (state is ReaderPageState.Success) {
        val bytes = state.bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            imageBitmap = bitmap.asImageBitmap()
        }

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

    BoxForImageState(
        modifier = modifier
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .onGloballyPositioned { coordinates ->
                val heightDp = with(density) { coordinates.size.height.toDp() }
                if (heightDp > 0.dp) {
                    ImageHeightCache.setHeight(page.url, heightDp)
                }
            },
        state = page.state,
        imageBitmap = imageBitmap,
        index = index,
        minHeight = cachedHeight ?: 200.dp,
        totalImages = totalImages
    )
}

@Composable
private fun BoxForImageState(
    modifier: Modifier,
    state: ReaderPageState,
    imageBitmap: ImageBitmap?,
    index: Int,
    minHeight: Dp,
    totalImages: Int,
) {
    when (state) {
        is ReaderPageState.Loading -> {
            ImageLoadingComposable(index = index, minHeight = minHeight)
        }

        is ReaderPageState.Error -> {
            ImageLoadingErrorComposable(index = index)
        }

        is ReaderPageState.Success -> {
            if (imageBitmap == null) {
                ImageLoadingErrorComposable(index = index)
                return
            }
            Image(
                bitmap = imageBitmap,
                contentDescription = "Page ${index + 1} of $totalImages",
                modifier = modifier,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
