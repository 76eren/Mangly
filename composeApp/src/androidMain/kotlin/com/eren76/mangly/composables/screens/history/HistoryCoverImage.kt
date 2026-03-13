package com.eren76.mangly.composables.screens.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun HistoryCoverImage(
    historyEntity: HistoryEntity,
    source: Source?,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    if (source == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        return
    }

    HistoryCoverImageLoader(
        historyEntity = historyEntity,
        source = source,
        historyViewModel = historyViewModel,
        modifier = modifier
    )
}

@Composable
private fun HistoryCoverImageLoader(
    historyEntity: HistoryEntity,
    source: Source,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var localCoverFile by remember(historyEntity.id, historyEntity.coverImageFilename) {
        mutableStateOf<File?>(null)
    }

    LaunchedEffect(historyEntity.id, historyEntity.coverImageFilename) {
        localCoverFile = historyEntity.coverImageFilename?.let { filename ->
            historyViewModel.getCoverFile(filename = filename, context = context)
        }
    }

    var imageForList by remember(historyEntity.id, historyEntity.mangaUrl) {
        mutableStateOf<Source.ImageForChaptersList?>(null)
    }
    var loadFailed by remember(historyEntity.id, historyEntity.mangaUrl) { mutableStateOf(false) }

    LaunchedEffect(historyEntity.id, historyEntity.mangaUrl, localCoverFile) {
        if (localCoverFile != null) return@LaunchedEffect

        loadFailed = false
        imageForList = null

        val coverInfo: Source.ImageForChaptersList? = runCatching {
            withContext(Dispatchers.IO) { source.getImageForChaptersList(historyEntity.mangaUrl) }
        }.getOrNull()

        if (coverInfo == null || coverInfo.imageUrl.isBlank()) {
            loadFailed = true
            return@LaunchedEffect
        }

        imageForList = coverInfo

        val downloaded = runCatching {
            CoverCache.downloadImage(coverInfo.imageUrl, coverInfo.headers)
        }.getOrNull()

        if (downloaded == null || downloaded.bytes.isEmpty()) {
            loadFailed = true
            return@LaunchedEffect
        }

        val ext = CoverCache.inferImageExtension(
            contentType = downloaded.contentType,
            finalUrl = downloaded.finalUrl,
            originalUrl = coverInfo.imageUrl
        )

        val filename = "${historyEntity.id}.$ext"

        val savedFile = withContext(Dispatchers.IO) {
            historyViewModel.saveCoverBytes(
                filename = filename,
                bytes = downloaded.bytes,
                context = context
            )
        }

        historyViewModel.updateHistoryCoverFilename(
            historyId = historyEntity.id,
            filename = filename
        )

        localCoverFile = savedFile
    }

    val localRequest = remember(localCoverFile) {
        CoverImageRequests.local(context = context, file = localCoverFile)
    }

    val networkHeaders = remember(imageForList?.headers) {
        CoverCache.buildNetworkHeaders(imageForList?.headers ?: emptyList())
    }

    val remoteRequest = remember(imageForList?.imageUrl, networkHeaders) {
        CoverImageRequests.remote(
            context = context,
            imageForList = imageForList,
            networkHeaders = networkHeaders,
            crossfade = true
        )
    }

    when {
        loadFailed -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                ImageLoadingErrorComposable()
            }
        }

        localRequest != null -> {
            SubcomposeAsyncImage(
                model = localRequest,
                contentDescription = historyEntityDescriptionFromUrl(historyEntity.mangaUrl),
                modifier = modifier,
                contentScale = ContentScale.Crop,
                loading = { ImageLoadingComposable() },
                error = { ImageLoadingErrorComposable() }
            )
        }

        imageForList?.imageUrl == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                ImageLoadingComposable()
            }
        }

        else -> {
            SubcomposeAsyncImage(
                model = remoteRequest,
                contentDescription = historyEntityDescriptionFromUrl(historyEntity.mangaUrl),
                modifier = modifier,
                contentScale = ContentScale.Crop,
                loading = { ImageLoadingComposable() },
                error = { ImageLoadingErrorComposable() }
            )
        }
    }
}

private fun historyEntityDescriptionFromUrl(url: String): String = "Cover for $url"

