package org.example.project.Composables.Standard.Read.viewer.webtoon

import org.example.project.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonImage(
    imageUrl: String,
    headers: List<Source.Header>,
    contentDescription: String,
) {
    val request = buildImageRequest(imageUrl, headers)

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
        placeholder = painterResource(R.drawable.ic_launcher_background),
    )
}

