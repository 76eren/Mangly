package org.example.project.Composables.Standard.Read.viewer.webtoon

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun rememberStrongImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .build()
    }
}

fun buildImageRequest(
    context: Context,
    url: String,
    headers: List<com.example.manglyextension.plugins.Source.Header>
): ImageRequest {
    val networkHeaders = NetworkHeaders.Builder().apply {
        for (header in headers) {
            this[header.name] = header.value
        }
    }.build()

    return ImageRequest.Builder(context)
        .data(url)
        .httpHeaders(networkHeaders)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()
}
