package com.eren76.mangly.composables.screens.readviewer.paged

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState

@Composable
fun PagedImage(
    page: ReaderPage,
    index: Int,
    totalImages: Int
) {
    val imageBitmap: ImageBitmap? = remember(page.state) {
        val bytes = (page.state as? ReaderPageState.Success)?.bytes ?: return@remember null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom and pan when image changes (new chapter/page)
    LaunchedEffect(page.url) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(
                                -500f * (scale - 1),
                                500f * (scale - 1)
                            ),
                            y = (offset.y + pan.y).coerceIn(-500f * (scale - 1), 500f * (scale - 1))
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (page.state) {
            is ReaderPageState.Loading -> ImageLoadingComposable(index = index)
            is ReaderPageState.Error -> ImageLoadingErrorComposable(index = index)
            is ReaderPageState.Success -> {
                if (imageBitmap == null) {
                    ImageLoadingErrorComposable(index = index)
                } else {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Page ${index + 1} of $totalImages",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}



