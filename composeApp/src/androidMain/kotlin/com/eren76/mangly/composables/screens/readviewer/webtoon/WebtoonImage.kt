package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * We split anything taller than MAX_TEXTURE_SIZE into tiles that each stay within this budget.
 */
private const val MAX_TEXTURE_SIZE = 4096

object ImageHeightCache {
    private val heightCache = HashMap<String, Dp>()

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


    val tiles: List<ImageBitmap>? by produceState<List<ImageBitmap>?>(
        initialValue = null,
        key1 = page.state,
    ) {
        value = when (val state = page.state) {
            is ReaderPageState.Success -> withContext(Dispatchers.Default) {
                runCatching {
                    val src: Bitmap =
                        BitmapFactory.decodeByteArray(state.bytes, 0, state.bytes.size)
                            ?: return@runCatching null

                    if (src.height <= MAX_TEXTURE_SIZE) {
                        // Common case: image fits in a single GPU texture – no copy needed.
                        listOf(src.asImageBitmap())
                    } else {
                        // Tall page: split into MAX_TEXTURE_SIZE-px tall tiles so every tile is within the GPU texture size limit.
                        buildList {
                            var y = 0
                            while (y < src.height) {
                                val tileHeight = minOf(MAX_TEXTURE_SIZE, src.height - y)
                                val tile = Bitmap.createBitmap(src, 0, y, src.width, tileHeight)
                                add(tile.asImageBitmap())
                                y += tileHeight
                            }
                            src.recycle()
                        }
                    }
                }.getOrNull()
            }

            else -> null
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

    PageContent(
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
        tiles = tiles,
        index = index,
        minHeight = cachedHeight ?: 200.dp,
        totalImages = totalImages,
    )
}

@Composable
private fun PageContent(
    modifier: Modifier,
    state: ReaderPageState,
    tiles: List<ImageBitmap>?,
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
            when {
                tiles == null -> ImageLoadingComposable(index = index, minHeight = minHeight)

                tiles.isEmpty() -> ImageLoadingErrorComposable(index = index)

                // Single tile – common case, same as before.
                tiles.size == 1 -> Image(
                    bitmap = tiles[0],
                    contentDescription = "Page ${index + 1} of $totalImages",
                    modifier = modifier,
                    contentScale = ContentScale.FillWidth,
                )

                else -> Column(modifier = modifier) {
                    tiles.forEachIndexed { tileIndex, tile ->
                        Image(
                            bitmap = tile,
                            contentDescription = if (tileIndex == 0) "Page ${index + 1} of $totalImages" else null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            }
        }
    }
}