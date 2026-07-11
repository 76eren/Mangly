package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun HomeCoverImage(
    model: Any?,
    contentDescription: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember(model) { mutableStateOf(false) }
    var imageLoaded by remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageLoaded) {
            Text(
                text = if (!imageLoadFailed && (isLoading || model != null)) {
                    "Loading cover..."
                } else {
                    "Cover unavailable"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoaded = true },
                onError = {
                    imageLoaded = false
                    imageLoadFailed = true
                }
            )
        }
    }
}
