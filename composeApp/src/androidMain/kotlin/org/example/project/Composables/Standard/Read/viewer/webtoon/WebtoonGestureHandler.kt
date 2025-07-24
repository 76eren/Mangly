package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.PointerInputScope

suspend fun PointerInputScope.detectWebtoonGestures(state: WebtoonReaderState) {
    detectTransformGestures { _, _, zoom, _ ->
        state.scale = (state.scale * zoom).coerceIn(1f, 3f)
    }
}
