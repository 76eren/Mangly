package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.Source

@Composable
fun buildImageRequest(url: String, headers: List<Source.Header>): ImageRequest {
    val networkHeaders = NetworkHeaders.Builder().apply {
        for (header in headers) {
            this[header.name] = header.value
        }
    }.build()

    return ImageRequest.Builder(LocalContext.current)
        .data(url)
        .httpHeaders(networkHeaders)
        .crossfade(true)
        .build()
}
