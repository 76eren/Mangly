package com.eren76.mangly.composables.screens.readviewer

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

suspend fun loadReaderPagesIncrementally(
    context: Context,
    imageLoader: ImageLoader,
    pages: MutableList<ReaderPage>,
    headers: NetworkHeaders,
) = coroutineScope {
    val maxConcurrency: Int = 4 // TODO: Make this user-configurable
    val semaphore = Semaphore(maxConcurrency)

    pages.mapIndexed { index, page ->
        async {
            semaphore.withPermit {
                if (pages.getOrNull(index)?.state is ReaderPageState.Success) return@withPermit

                try {
                    val request = ImageRequest.Builder(context)
                        .data(page.url)
                        .httpHeaders(headers)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .bitmapConfig(Bitmap.Config.ARGB_8888)
                        .size(Size.ORIGINAL)
                        .build()

                    val result: ImageResult = withContext(Dispatchers.IO) {
                        imageLoader.execute(request)
                    }
                    when (result) {
                        is SuccessResult -> {
                            val bitmap = result.image.toBitmap()
                            val bytes = withContext(Dispatchers.IO) {
                                ByteArrayOutputStream().use { baos ->
                                    bitmap.compress(
                                        Bitmap.CompressFormat.PNG,
                                        100,
                                        baos
                                    )
                                    baos.toByteArray()
                                }
                            }

                            pages[index] = page.copy(state = ReaderPageState.Success(bytes))
                        }

                        is ErrorResult -> {
                            pages[index] =
                                page.copy(state = ReaderPageState.Error(result.throwable))
                        }
                    }
                } catch (t: Throwable) {
                    pages[index] = page.copy(state = ReaderPageState.Error(t))
                }
            }
        }
    }.awaitAll()
}



