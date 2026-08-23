package com.eren76.mangly.composables.screens.readviewer.paged

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PagedImage(
    page: ReaderPage,
    index: Int,
    totalImages: Int,
    zoomState: PagedZoomState,
    onRetry: () -> Unit
) {
    val bitmapState = rememberPageBitmapState(page.state)
    val imageBitmap = (bitmapState as? PageBitmapState.Ready)?.bitmap

    LaunchedEffect(imageBitmap) {
        val bitmap = imageBitmap ?: return@LaunchedEffect
        zoomState.updateImageSize(IntSize(bitmap.width, bitmap.height))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged(zoomState::updateViewportSize)
            .pagedZoomGestures(
                pageKey = page.url,
                zoomState = zoomState
            ),
        contentAlignment = Alignment.Center
    ) {
        when (bitmapState) {
            PageBitmapState.Loading -> ImageLoadingComposable(index = index)
            PageBitmapState.Error -> ImageLoadingErrorComposable(
                index = index,
                onRetry = onRetry
            )
            is PageBitmapState.Ready -> Image(
                bitmap = bitmapState.bitmap,
                contentDescription = "Page ${index + 1} of $totalImages",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomState.scale,
                        scaleY = zoomState.scale,
                        translationX = zoomState.offset.x,
                        translationY = zoomState.offset.y
                    ),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun rememberPageBitmapState(pageState: ReaderPageState): PageBitmapState {
    val bitmapState by produceState<PageBitmapState>(
        initialValue = PageBitmapState.Loading,
        key1 = pageState
    ) {
        value = when (pageState) {
            ReaderPageState.Loading -> PageBitmapState.Loading
            is ReaderPageState.Error -> PageBitmapState.Error
            is ReaderPageState.Success -> decodePageBitmap(pageState.bytes)
        }
    }

    return bitmapState
}

private fun Modifier.pagedZoomGestures(
    pageKey: String,
    zoomState: PagedZoomState
): Modifier = pointerInput(pageKey, zoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }
            val pan = event.calculatePan()

            when {
                pressedPointers > 1 -> {
                    zoomState.applyTransform(
                        centroid = event.calculateCentroid(useCurrent = false),
                        pan = pan,
                        zoomChange = event.calculateZoom()
                    )
                    event.changes.consumePositionChanges()
                }

                zoomState.isZoomed -> {
                    zoomState.panBy(pan)
                    event.changes.consumePositionChanges()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

private sealed interface PageBitmapState {
    data object Loading : PageBitmapState
    data object Error : PageBitmapState
    data class Ready(val bitmap: ImageBitmap) : PageBitmapState
}

private suspend fun decodePageBitmap(bytes: ByteArray): PageBitmapState {
    return withContext(Dispatchers.Default) {
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()

        if (bitmap == null) PageBitmapState.Error else PageBitmapState.Ready(bitmap)
    }
}

private fun List<PointerInputChange>.consumePositionChanges() {
    forEach { change ->
        if (change.positionChanged()) {
            change.consume()
        }
    }
}


