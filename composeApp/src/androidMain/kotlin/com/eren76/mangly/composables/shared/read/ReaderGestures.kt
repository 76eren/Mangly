package com.eren76.mangly.composables.shared.read

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

private const val SIDE_ZONE_FRACTION = 1f / 3f

private sealed interface ReaderPress {
    data object Cancelled : ReaderPress
    data class Tap(
        val position: Offset,
        val uptimeMillis: Long,
        val controlsTapEnabled: Boolean
    ) : ReaderPress

    data class LongPress(val position: Offset) : ReaderPress
}

@Composable
fun Modifier.readerGestures(
    onControlsTap: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    isDoubleTapEnabled: Boolean = true,
    isControlsTapEnabled: () -> Boolean = { true }
): Modifier {
    val currentOnControlsTap by rememberUpdatedState(onControlsTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentIsDoubleTapEnabled by rememberUpdatedState(isDoubleTapEnabled)
    val currentIsControlsTapEnabled by rememberUpdatedState(isControlsTapEnabled)

    return pointerInput(Unit) {
        var previousCenterTapTime: Long? = null
        var previousCenterTapPosition: Offset? = null
        val maximumDoubleTapDistance = 64.dp.toPx()

        awaitEachGesture {
            when (val press = awaitReaderPress(currentIsControlsTapEnabled)) {
                ReaderPress.Cancelled -> {
                    previousCenterTapTime = null
                    previousCenterTapPosition = null
                }

                is ReaderPress.LongPress -> {
                    currentOnLongPress(press.position)
                    previousCenterTapTime = null
                    previousCenterTapPosition = null
                }

                is ReaderPress.Tap -> {
                    val shouldToggleControls =
                        !currentIsDoubleTapEnabled || press.position.isInSideZone(size.width)

                    if (shouldToggleControls) {
                        if (press.controlsTapEnabled) {
                            currentOnControlsTap()
                        }
                        previousCenterTapTime = null
                        previousCenterTapPosition = null
                        return@awaitEachGesture
                    }

                    val previousTime = previousCenterTapTime
                    val previousPosition = previousCenterTapPosition
                    val timeSincePreviousTap = previousTime?.let(press.uptimeMillis::minus)
                    val distanceFromPreviousTap = previousPosition
                        ?.let { (press.position - it).getDistance() }

                    val isDoubleTap =
                        timeSincePreviousTap != null &&
                                timeSincePreviousTap >= viewConfiguration.doubleTapMinTimeMillis &&
                                timeSincePreviousTap <= viewConfiguration.doubleTapTimeoutMillis &&
                                distanceFromPreviousTap != null &&
                                distanceFromPreviousTap <= maximumDoubleTapDistance

                    if (isDoubleTap) {
                        currentOnDoubleTap(press.position)
                        previousCenterTapTime = null
                        previousCenterTapPosition = null
                    } else {
                        previousCenterTapTime = press.uptimeMillis
                        previousCenterTapPosition = press.position
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitReaderPress(
    isControlsTapEnabled: () -> Boolean
): ReaderPress {
    val down = awaitFirstDown(
        requireUnconsumed = false,
        pass = PointerEventPass.Initial
    )
    val controlsTapEnabled = isControlsTapEnabled()

    return withTimeoutOrNull(
        timeMillis = viewConfiguration.longPressTimeoutMillis
    ) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val trackedChange = event.changes.firstOrNull { it.id == down.id }

            if (
                trackedChange == null ||
                event.changes.count { it.pressed } > 1 ||
                (trackedChange.position - down.position).getDistance() > viewConfiguration.touchSlop
            ) {
                return@withTimeoutOrNull ReaderPress.Cancelled
            }

            if (!trackedChange.pressed) {
                return@withTimeoutOrNull ReaderPress.Tap(
                    position = trackedChange.position,
                    uptimeMillis = trackedChange.uptimeMillis,
                    controlsTapEnabled = controlsTapEnabled
                )
            }
        }

        ReaderPress.Cancelled
    } ?: ReaderPress.LongPress(down.position)
}

private fun Offset.isInSideZone(screenWidth: Int): Boolean {
    val sideZoneWidth = screenWidth * SIDE_ZONE_FRACTION
    return x < sideZoneWidth || x > screenWidth - sideZoneWidth
}
