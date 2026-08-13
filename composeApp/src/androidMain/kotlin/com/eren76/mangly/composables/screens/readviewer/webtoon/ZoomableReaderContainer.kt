package com.eren76.mangly.composables.screens.readviewer.webtoon

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

private const val MINIMUM_ZOOM = 1f
private const val MAXIMUM_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val ZOOM_TOLERANCE = 0.01f

@Stable
internal class WebtoonZoomState {
    var scale by mutableFloatStateOf(MINIMUM_ZOOM)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize = IntSize.Zero

    val isZoomed: Boolean
        get() = scale > MINIMUM_ZOOM + ZOOM_TOLERANCE

    private val hasZoomTransform: Boolean
        get() = scale > MINIMUM_ZOOM || offset != Offset.Zero

    fun updateViewportSize(size: IntSize) {
        if (viewportSize == size) return

        viewportSize = size
        offset = offset.coerceToBounds(scale)
    }

    fun transformBy(
        centroid: Offset,
        pan: Offset,
        zoomChange: Float
    ) {
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(MINIMUM_ZOOM, MAXIMUM_ZOOM)
        val transformedOffset = calculateOffsetForZoom(
            position = centroid,
            oldScale = oldScale,
            newScale = newScale
        ) + pan

        scale = newScale
        offset = if (newScale <= MINIMUM_ZOOM + ZOOM_TOLERANCE) {
            Offset.Zero
        } else {
            transformedOffset.coerceToBounds(newScale)
        }
    }

    fun panBy(pan: Offset) {
        offset = (offset + pan).coerceToBounds(scale)
    }

    fun toggleZoomAt(position: Offset) {
        if (hasZoomTransform) {
            reset()
            return
        }

        offset = calculateOffsetForZoom(
            position = position,
            oldScale = scale,
            newScale = DOUBLE_TAP_ZOOM
        ).coerceToBounds(DOUBLE_TAP_ZOOM)
        scale = DOUBLE_TAP_ZOOM
    }

    fun reset() {
        scale = MINIMUM_ZOOM
        offset = Offset.Zero
    }

    fun viewportToContent(position: Offset): Offset {
        val center = viewportCenter()
        return center + (position - center - offset) / scale
    }

    private fun calculateOffsetForZoom(
        position: Offset,
        oldScale: Float,
        newScale: Float
    ): Offset {
        val zoomChange = newScale / oldScale
        val focusPoint = position - viewportCenter()
        return offset * zoomChange + focusPoint * (1f - zoomChange)
    }

    private fun Offset.coerceToBounds(scale: Float): Offset {
        val maxOffset = Offset(
            x = ((scale - MINIMUM_ZOOM) * viewportSize.width) / 2f,
            y = ((scale - MINIMUM_ZOOM) * viewportSize.height) / 2f
        )
        return Offset(
            x = x.coerceIn(-maxOffset.x, maxOffset.x),
            y = y.coerceIn(-maxOffset.y, maxOffset.y)
        )
    }

    private fun viewportCenter(): Offset {
        return Offset(
            x = viewportSize.width / 2f,
            y = viewportSize.height / 2f
        )
    }
}

@Composable
internal fun ZoomableReaderContainer(
    zoomState: WebtoonZoomState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .onSizeChanged(zoomState::updateViewportSize)
            .clipToBounds()
            .webtoonZoomGestures(zoomState)
            .graphicsLayer {
                scaleX = zoomState.scale
                scaleY = zoomState.scale
                translationX = zoomState.offset.x
                translationY = zoomState.offset.y
            },
        content = content
    )
}

private fun Modifier.webtoonZoomGestures(zoomState: WebtoonZoomState): Modifier {
    return pointerInput(zoomState) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)

            do {
                val event = awaitPointerEvent()
                val pressedPointers = event.changes.count { it.pressed }
                val pan = event.calculatePan()

                when {
                    pressedPointers > 1 -> {
                        zoomState.transformBy(
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
}

private fun List<PointerInputChange>.consumePositionChanges() {
    forEach { change ->
        if (change.positionChanged()) {
            change.consume()
        }
    }
}
