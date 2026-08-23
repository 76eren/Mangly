package com.eren76.mangly.composables.shared.read

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset

private const val DOUBLE_TAP_ZOOM_DURATION_MILLIS = 220

internal data class ZoomTransform(
    val scale: Float,
    val offset: Offset
)

internal class ZoomAnimator {
    private var animationId = 0L

    fun cancel() {
        animationId++
    }

    suspend fun animateTo(
        from: ZoomTransform,
        to: ZoomTransform,
        onFrame: (ZoomTransform) -> Unit
    ) {
        val currentAnimationId = ++animationId

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = DOUBLE_TAP_ZOOM_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        ) { progress, _ ->
            if (currentAnimationId != animationId) return@animate

            onFrame(
                ZoomTransform(
                    scale = from.scale.interpolateTo(to.scale, progress),
                    offset = from.offset.interpolateTo(to.offset, progress)
                )
            )
        }
    }
}

private fun Float.interpolateTo(target: Float, progress: Float): Float {
    return this + (target - this) * progress
}

private fun Offset.interpolateTo(target: Offset, progress: Float): Offset {
    return this + (target - this) * progress
}
