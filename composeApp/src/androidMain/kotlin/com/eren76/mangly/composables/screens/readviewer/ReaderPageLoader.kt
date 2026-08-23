package com.eren76.mangly.composables.screens.readviewer

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
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
import kotlinx.coroutines.CancellationException

suspend fun loadReaderPagesIncrementally(
    context: Context,
    imageLoader: ImageLoader,
    pages: MutableList<ReaderPage>,
    headers: NetworkHeaders,
) = coroutineScope {
    val maxConcurrency: Int = 4 // TODO: Make this user-configurable
    val semaphore = Semaphore(maxConcurrency)

    pages.indices.map { index ->
        async {
            semaphore.withPermit {
                if (pages.getOrNull(index)?.state is ReaderPageState.Success) return@withPermit
                loadReaderPage(context, imageLoader, pages, index, headers)
            }
        }
    }.awaitAll()
}

suspend fun loadReaderPage(
    context: Context,
    imageLoader: ImageLoader,
    pages: MutableList<ReaderPage>,
    pageIndex: Int,
    headers: NetworkHeaders
) {
    val page = pages.getOrNull(pageIndex) ?: return
    pages[pageIndex] = page.copy(state = ReaderPageState.Loading)

    val state = try {
        val request = ImageRequest.Builder(context)
            .data(page.url)
            .httpHeaders(headers)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowHardware(false)
            .size(Size.ORIGINAL)
            .build()

        when (val result = withContext(Dispatchers.IO) { imageLoader.execute(request) }) {
            is SuccessResult -> ReaderPageState.Success(result.readBytes(imageLoader))
            is ErrorResult -> ReaderPageState.Error(result.throwable)
        }
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        ReaderPageState.Error(throwable)
    }

    val currentPage = pages.getOrNull(pageIndex) ?: return
    if (currentPage.url == page.url) {
        pages[pageIndex] = currentPage.copy(state = state)
    }
}

private suspend fun SuccessResult.readBytes(imageLoader: ImageLoader): ByteArray {
    val rawBytes = withContext(Dispatchers.IO) {
        diskCacheKey?.let { key ->
            imageLoader.diskCache?.openSnapshot(key)?.use { snapshot ->
                runCatching { snapshot.data.toFile().readBytes() }.getOrNull()
            }
        }
    }
    if (rawBytes != null) return rawBytes

    return withContext(Dispatchers.IO) {
        ByteArrayOutputStream().use { output ->
            image.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }
}
