package com.eren76.mangly.composables.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel

@Composable
fun DownloadCard(
    downloadWithChapters: DownloadWithChapters,
    downloadsViewModel: DownloadsViewModel,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val download = downloadWithChapters.download
    val title = download.mangaName ?: download.mangaUrl
    val downloadedChapters = downloadWithChapters.chapters.count { it.isFullyDownloaded }
    val subtitle = "$downloadedChapters chapters downloaded"

    HomeMangaCard(
        title = title,
        subtitle = subtitle,
        menuKey = download.downloadId,
        menuText = "Delete download",
        onClick = onClick,
        onDelete = {
            downloadsViewModel.deleteWholeMangaDownloadByDownloadEntityId(
                download.downloadId,
                context
            )
            Toast.makeText(context, "Download deleted", Toast.LENGTH_SHORT).show()
        },
        imageContent = { modifier ->
            DownloadCoverImage(
                downloadWithChapters = downloadWithChapters,
                downloadsViewModel = downloadsViewModel,
                modifier = modifier
            )
        }
    )
}

@Composable
fun DownloadCoverImage(
    downloadWithChapters: DownloadWithChapters,
    downloadsViewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val download = downloadWithChapters.download
    val title = download.mangaName ?: download.mangaUrl

    val localCoverFile = remember(download.downloadId, download.coverImageFilename, context) {
        download.coverImageFilename.takeIf { it.isNotBlank() }?.let { filename ->
            downloadsViewModel.getCoverFile(filename = filename, context = context)
        }
    }

    val localRequest = remember(localCoverFile) {
        CoverImageRequests.local(context = context, file = localCoverFile)
    }

    if (localRequest == null) {
        Box(
            modifier = modifier.background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            ImageLoadingErrorComposable()
        }
        return
    }

    SubcomposeAsyncImage(
        model = localRequest,
        contentDescription = title,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        error = { ImageLoadingErrorComposable() },
        loading = { ImageLoadingComposable() }
    )
}
