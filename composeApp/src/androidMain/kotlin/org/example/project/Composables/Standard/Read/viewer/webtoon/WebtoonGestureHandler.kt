package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.PointerInputScope

suspend fun PointerInputScope.detectWebtoonGestures(state: WebtoonReaderState) {
    detectTransformGestures { _, pan, zoom, _ ->
        state.scale = (state.scale * zoom).coerceIn(0.5f, 3f)
        state.offset += pan
    }
}
