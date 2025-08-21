package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonReader(
    images: List<String>,
    headers: List<Source.Header>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        WebtoonLazyColumn(
            images = images,
            headers = headers,
        )
    }
}
