package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class WebtoonReaderState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberWebtoonReaderState(): WebtoonReaderState = remember { WebtoonReaderState() }
