package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonLazyColumn(
    images: List<String>,
    headers: List<Source.Header>,
    state: WebtoonReaderState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = state.scale,
                scaleY = state.scale,
                translationX = state.offset.x,
                translationY = state.offset.y
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(images.size) { index ->
            WebtoonImage(
                imageUrl = images[index],
                headers = headers,
                contentDescription = "Chapter image ${index + 1}",
                scale = state.scale
            )
        }
    }
}
