package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.shared.read.ReadBottomControls
import com.eren76.mangly.composables.shared.read.ReadTopControls
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.launch

object WebtoonReaderMode : ReaderMode {
    override val name: String = "Webtoon"

    @Composable
    override fun Content(
        images: List<String>,
        headers: List<Source.Header>,
        modifier: Modifier,
        onPreviousChapter: () -> Unit,
        onNextChapter: () -> Unit,
        chaptersListViewModel: ChaptersListViewModel
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()


        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )

        val lazyListState = rememberLazyListState()

        val networkHeaders = remember(headers) {
            NetworkHeaders.Builder().apply {
                for (header in headers) {
                    this[header.name] = header.value
                }
            }.build()
        }

        PrefetchAroundViewport(
            lazyListState = lazyListState,
            images = images,
            networkHeaders = networkHeaders,
            sharedPreferences = sharedPreferences
        )


        var showControls by remember { mutableStateOf(false) }

        val currentPage by remember {
            derivedStateOf {
                val firstVisibleItem = lazyListState.firstVisibleItemIndex

                // Account for header item
                if (firstVisibleItem > 0) firstVisibleItem else 1
            }
        }

        // Reset scroll position when images change (new chapter)
        LaunchedEffect(images) {
            ImageHeightCache.clear()
            lazyListState.scrollToItem(0)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        ) {
            ZoomableReaderContainer(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Header with chapter title
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chaptersListViewModel.getSelectedChapterNumber(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    items(
                        count = images.size,
                        key = { index -> "image_$index" }
                    ) { index ->
                        val imageUrl = images[index]

                        WebtoonImage(
                            imageUrl = imageUrl,
                            networkHeaders = networkHeaders,
                            context = context,
                            index = index,
                            totalImages = images.size
                        )
                    }

                    // Footer with navigation
                    item {
                        ChapterNavigationFooter(
                            onPreviousChapter = onPreviousChapter,
                            onNextChapter = onNextChapter,
                            chaptersListViewModel = chaptersListViewModel
                        )
                    }
                }

                // Overlay controls (now also zoomed with everything else)
                if (showControls) {
                    ReadTopControls(
                        currentPage = currentPage,
                        totalPages = images.size,
                        chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                        onPreviousChapter = onPreviousChapter,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    ReadBottomControls(
                        onNextChapter = onNextChapter,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        currentPage = currentPage,
                        totalPages = images.size,
                        onGoToPage = { page ->
                            coroutineScope.launch {
                                lazyListState.scrollToItem(page)
                            }
                        }

                    )
                }
            }
        }

    }
}

@Composable
private fun PrefetchAroundViewport(
    lazyListState: LazyListState,
    images: List<String>,
    networkHeaders: NetworkHeaders,
    sharedPreferences: SharedPreferences
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader(context)

    val preloadAmount = sharedPreferences.getInt(
        ReaderModePrefs.IMAGE_PRELOAD_AMOUNT,
        2
    )

    LaunchedEffect(images, networkHeaders) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisible ->

                // +1 because item 0 is header
                val currentImageIndex = (firstVisible - 1).coerceAtLeast(0)

                val start = (currentImageIndex).coerceAtLeast(0)
                val endExclusive = (currentImageIndex + preloadAmount).coerceAtMost(images.size)

                for (i in start until endExclusive) {
                    val url = images[i]
                    val req = ImageRequest.Builder(context)
                        .data(url)
                        .httpHeaders(networkHeaders)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()

                    imageLoader.enqueue(req)
                }
            }
    }
}