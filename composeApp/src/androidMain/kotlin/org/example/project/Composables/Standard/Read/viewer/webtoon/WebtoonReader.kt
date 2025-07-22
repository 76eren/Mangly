package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonReader(
    images: List<String>,
    headers: List<Source.Header>,
    modifier: Modifier = Modifier
) {
    val state = rememberWebtoonReaderState()

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectWebtoonGestures(state)
            }
    ) {
        WebtoonLazyColumn(
            images = images,
            headers = headers,
            state = state
        )
    }
}
