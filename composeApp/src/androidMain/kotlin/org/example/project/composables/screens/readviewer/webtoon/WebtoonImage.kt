package org.example.project.composables.screens.readviewer.webtoon

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade

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
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Page ${index + 1} of $totalImages",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
        loading = {
            WebtoonImageLoading(index = index)
        },
        error = {
            WebtoonImageError(index = index)
        }
    )
}

@Composable
fun WebtoonImageLoading(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading page ${index + 1}...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun WebtoonImageError(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Failed to load image",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Page ${index + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

