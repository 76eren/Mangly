package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable

@Composable
fun WebtoonImage(
    imageUrl: String,
    networkHeaders: NetworkHeaders,
    context: Context,
    index: Int,
    totalImages: Int
) {
    val imageRequest = remember(imageUrl, networkHeaders) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(networkHeaders)
            .crossfade(false)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Page ${index + 1} of $totalImages",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
        loading = {
            ImageLoadingComposable(index = index)
        },
        error = {
            ImageLoadingErrorComposable(index = index)
        }
    )
}



