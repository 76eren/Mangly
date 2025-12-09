package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonImage(
    imageUrl: String,
    headers: List<Source.Header>,
    contentDescription: String,
) {
    val request = buildImageRequest(imageUrl, headers)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp)
            .clipToBounds()
            .graphicsLayer { clip = true }
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}
