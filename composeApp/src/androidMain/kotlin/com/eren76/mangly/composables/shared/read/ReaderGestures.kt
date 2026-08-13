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

private const val SIDE_ZONE_FRACTION = 1f / 3f

private sealed interface ReaderPress {
    data object Cancelled : ReaderPress
    data class Tap(
        val position: Offset,
        val uptimeMillis: Long,
        val sideTapEnabled: Boolean
    ) : ReaderPress

    data class LongPress(val position: Offset) : ReaderPress
}

@Composable
fun Modifier.readerGestures(
    onSideTap: () -> Unit,
    onCenterDoubleTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    isSideTapEnabled: () -> Boolean = { true }
): Modifier {
    val currentOnSideTap by rememberUpdatedState(onSideTap)
    val currentOnCenterDoubleTap by rememberUpdatedState(onCenterDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentIsSideTapEnabled by rememberUpdatedState(isSideTapEnabled)

    return pointerInput(Unit) {
        var previousCenterTapTime: Long? = null
        var previousCenterTapPosition: Offset? = null
        val maximumDoubleTapDistance = 64.dp.toPx()

        awaitEachGesture {
            when (val press = awaitReaderPress(currentIsSideTapEnabled)) {
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
                    if (press.position.isInSideZone(size.width)) {
                        if (press.sideTapEnabled) {
                            currentOnSideTap()
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
                        currentOnCenterDoubleTap(press.position)
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
    isSideTapEnabled: () -> Boolean
): ReaderPress {
    val down = awaitFirstDown(
        requireUnconsumed = false,
        pass = PointerEventPass.Initial
    )
    val sideTapEnabled = isSideTapEnabled()

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
                    sideTapEnabled = sideTapEnabled
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
