package com.eren76.mangly.composables.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.manglyextension.plugins.Source

@Composable
fun SearchResultCard(
    searchResult: Source.SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            SearchResultImage(
                searchResult = searchResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = searchResult.title, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun SearchResultImage(
    searchResult: Source.SearchResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val networkHeaders = NetworkHeaders.Builder().apply {
        searchResult.headers.forEach { header ->
            this[header.name] = header.value
        }
    }.build()

    val imageRequest = ImageRequest.Builder(context)
        .data(searchResult.imageUrl)
        .httpHeaders(networkHeaders)
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = searchResult.title,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            ImageLoadingComposable()
        }
    )
}
