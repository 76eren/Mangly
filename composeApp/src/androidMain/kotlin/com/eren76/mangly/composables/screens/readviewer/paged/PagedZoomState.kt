package com.eren76.mangly.composables.screens.readviewer.paged

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

private const val MINIMUM_ZOOM = 1f
private const val MAXIMUM_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val ZOOM_TOLERANCE = 0.01f

@Stable
internal class PagedZoomState {
    var scale by mutableFloatStateOf(MINIMUM_ZOOM)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize = IntSize.Zero
    private var imageSize = IntSize.Zero

    val isZoomed: Boolean
        get() = scale > MINIMUM_ZOOM + ZOOM_TOLERANCE

    private val hasZoomTransform: Boolean
        get() = scale > MINIMUM_ZOOM || offset != Offset.Zero

    fun updateViewportSize(size: IntSize) {
        if (viewportSize == size) return

        viewportSize = size
        offset = offset.coerceToBounds(scale)
    }

    fun updateImageSize(size: IntSize) {
        if (imageSize == size) return

        imageSize = size
        offset = offset.coerceToBounds(scale)
    }

    fun applyTransform(
        centroid: Offset,
        pan: Offset,
        zoomChange: Float
    ) {
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(MINIMUM_ZOOM, MAXIMUM_ZOOM)
        val transformedOffset = calculateOffsetForZoom(
            centroid = centroid,
            oldScale = oldScale,
            newScale = newScale,
            currentOffset = offset
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

    fun reset() {
        scale = MINIMUM_ZOOM
        offset = Offset.Zero
    }

    fun toggleZoomAt(position: Offset) {
        if (hasZoomTransform) {
            reset()
            return
        }

        offset = calculateOffsetForZoom(
            centroid = position,
            oldScale = scale,
            newScale = DOUBLE_TAP_ZOOM,
            currentOffset = offset
        ).coerceToBounds(DOUBLE_TAP_ZOOM)
        scale = DOUBLE_TAP_ZOOM
    }

    private fun calculateOffsetForZoom(
        centroid: Offset,
        oldScale: Float,
        newScale: Float,
        currentOffset: Offset
    ): Offset {
        val effectiveZoomChange = newScale / oldScale
        val viewportCenter = Offset(
            x = viewportSize.width / 2f,
            y = viewportSize.height / 2f
        )

        return currentOffset * effectiveZoomChange +
            (centroid - viewportCenter) * (1f - effectiveZoomChange)
    }

    private fun Offset.coerceToBounds(scale: Float): Offset {
        val bounds = panBounds(scale)
        return Offset(
            x = x.coerceIn(-bounds.x, bounds.x),
            y = y.coerceIn(-bounds.y, bounds.y)
        )
    }

    private fun panBounds(scale: Float): Offset {
        if (
            viewportSize.width == 0 ||
            viewportSize.height == 0 ||
            imageSize.width == 0 ||
            imageSize.height == 0
        ) {
            return Offset.Zero
        }

        val fittedScale = min(
            viewportSize.width.toFloat() / imageSize.width,
            viewportSize.height.toFloat() / imageSize.height
        )
        val scaledImageWidth = imageSize.width * fittedScale * scale
        val scaledImageHeight = imageSize.height * fittedScale * scale

        return Offset(
            x = ((scaledImageWidth - viewportSize.width) / 2f).coerceAtLeast(0f),
            y = ((scaledImageHeight - viewportSize.height) / 2f).coerceAtLeast(0f)
        )
    }
}
