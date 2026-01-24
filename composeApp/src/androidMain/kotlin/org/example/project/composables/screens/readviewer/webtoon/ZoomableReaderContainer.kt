package org.example.project.composables.screens.readviewer.webtoon


import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Composable
fun ZoomableReaderContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 4f

    fun clampOffset(s: Float, o: Offset): Offset {
        if (size.width == 0 || size.height == 0) return Offset.Zero

        val maxX = ((s - 1f) * size.width) / 2f
        val maxY = ((s - 1f) * size.height) / 2f

        return Offset(
            x = o.x.coerceIn(-maxX, maxX),
            y = o.y.coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        // Only allow pinch controls
                        if (changes.size >= 2) {
                            val zoomChange = event.calculateZoom()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

                                val containerCenter = Offset(size.width / 2f, size.height / 2f)
                                val focusPoint = centroid - containerCenter

                                val scaleRatio = newScale / scale
                                val newOffset =
                                    (offset + focusPoint) * scaleRatio - focusPoint + panChange

                                scale = newScale
                                offset = clampOffset(scale, newOffset)

                                if (scale <= 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                }

                                changes.forEach { it.consume() }
                            }
                        }
                        // This allows panning with one finger when zoomed in
                        // However this also unintentionally blocks click events when zoomed in
                        // I actually think this is acceptable behavior for now
                        else if (changes.size == 1 && scale > 1f) {
                            val change = changes.first()
                            if (change.positionChanged()) {
                                val pan = change.position - change.previousPosition
                                offset = clampOffset(scale, offset + pan)
                                change.consume()
                            }
                        }

                    } while (changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        content = content
    )
}

private fun PointerEvent.calculatePan(): Offset {
    val current = calculateCentroid(useCurrent = true)
    val previous = calculateCentroid(useCurrent = false)
    return current - previous
}