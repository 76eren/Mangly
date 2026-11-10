package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.manglyextension.plugins.Source.ImageForChaptersList
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import java.io.File

// Chapter cover with network image and headers
@Composable
fun ChapterCover(
    image: ImageForChaptersList,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(context, image) {
        CoverImageRequests.remote(context = context, imageForList = image)
    }

    ElevatedCard(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick?.invoke() })
        },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Comic Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            loading = { ImageLoadingComposable() },
            error = { ImageLoadingErrorComposable() }
        )
    }
}

// Chapter cover with local downloaded file no networking
@Composable
fun ChapterCover(
    coverFile: File,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick?.invoke() })
        },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        SubcomposeAsyncImage(
            model = coverFile,
            contentDescription = "Comic Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            loading = { ImageLoadingComposable() },
            error = { ImageLoadingErrorComposable() }
        )
    }
}
