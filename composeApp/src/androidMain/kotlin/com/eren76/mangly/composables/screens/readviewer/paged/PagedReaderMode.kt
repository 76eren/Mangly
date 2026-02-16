package com.eren76.mangly.composables.screens.readviewer.paged

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.network.NetworkHeaders
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.shared.read.ReadBottomControls
import com.eren76.mangly.composables.shared.read.ReadTopControls
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.launch

object PagedReaderMode : ReaderMode {
    override val name: String = "Paged"

    @OptIn(ExperimentalFoundationApi::class)
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

        val networkHeaders = remember(headers) {
            NetworkHeaders.Builder().apply {
                for (header in headers) {
                    this[header.name] = header.value
                }
            }.build()
        }

        // images + 1 navigation page at the end
        val totalPages = images.size + 1
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { totalPages }
        )

        var showControls by remember { mutableStateOf(false) }

        // Reset pager position when images change (new chapter)
        LaunchedEffect(images) {
            pagerState.scrollToPage(0)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val screenWidth = size.width
                            val tapX = offset.x

                            when {
                                // Tap on left third of screen -> go to previous page
                                tapX < screenWidth / 3 -> {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }
                                // Tap on right third of screen -> go to next page
                                tapX > screenWidth * 2 / 3 -> {
                                    if (pagerState.currentPage < totalPages - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                                // Tap on middle third -> show/hide controls
                                else -> {
                                    showControls = !showControls
                                }
                            }
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                if (pageIndex < images.size) {
                    // Regular image page
                    PagedImage(
                        imageUrl = images[pageIndex],
                        networkHeaders = networkHeaders,
                        context = context,
                        index = pageIndex,
                        totalImages = images.size
                    )
                } else {
                    // Navigation page at the end
                    PagedChapterNavigation(
                        onPreviousChapter = onPreviousChapter,
                        onNextChapter = onNextChapter,
                        chaptersListViewModel = chaptersListViewModel
                    )
                }
            }

            // Page indicator at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentPageDisplay = if (pagerState.currentPage < images.size) {
                    pagerState.currentPage + 1
                } else {
                    images.size
                }
                Text(
                    text = "$currentPageDisplay / ${images.size}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showControls) {
                ReadTopControls(
                    currentPage = if (pagerState.currentPage < images.size) pagerState.currentPage + 1 else images.size,
                    totalPages = images.size,
                    chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                    onPreviousChapter = onPreviousChapter,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ReadBottomControls(
                    onNextChapter = onNextChapter,
                    currentPage = pagerState.currentPage,
                    totalPages = totalPages,
                    onGoToPage = { page ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

