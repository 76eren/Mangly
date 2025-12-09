package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.example.manglyextension.plugins.Source

// TODO: Add zoom logic
@Composable
fun WebtoonLazyColumn(
    images: List<String>,
    headers: List<Source.Header>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(images) { index, imageUrl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                WebtoonImage(
                    imageUrl = imageUrl,
                    headers = headers,
                    contentDescription = "Chapter image ${index + 1}"
                )
            }
        }
    }
}
