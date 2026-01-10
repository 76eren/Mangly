package org.example.project.composables.screens.readviewer.webtoon

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.network.NetworkHeaders
import com.example.manglyextension.plugins.Source
import org.example.project.composables.screens.readviewer.ReaderMode
import org.example.project.viewmodels.ChaptersListViewModel

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
        val lazyListState = rememberLazyListState()

        val networkHeaders = remember(headers) {
            NetworkHeaders.Builder().apply {
                for (header in headers) {
                    this[header.name] = header.value
                }
            }.build()
        }

        val pager = remember(images) {
            Pager(
                config = PagingConfig(
                    // TODO: Currently these are hardcoded, I find these to be fine values for webtoon reader style however maybe make this customizable from the settings?
                    pageSize = 5,
                    prefetchDistance = 3,
                    enablePlaceholders = true,
                    initialLoadSize = 8
                ),
                pagingSourceFactory = {
                    WebtoonImagePagingSource(images)
                }
            )
        }

        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

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
            lazyListState.scrollToItem(0)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
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

                // Actual images
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> "image_$index" }
                ) { index ->
                    val imageUrl = lazyPagingItems[index]

                    if (imageUrl != null) {
                        WebtoonImage(
                            imageUrl = imageUrl,
                            networkHeaders = networkHeaders,
                            context = context,
                            index = index,
                            totalImages = images.size
                        )
                    } else {
                        // Placeholder while loading
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Footer with navigations
                item {
                    ChapterNavigationFooter(
                        onPreviousChapter = onPreviousChapter,
                        onNextChapter = onNextChapter,
                        chaptersListViewModel = chaptersListViewModel
                    )
                }
            }

            // Overlay controls when visible
            if (showControls) {
                WebtoonTopControls(
                    currentPage = currentPage,
                    totalPages = images.size,
                    chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                    onPreviousChapter = onPreviousChapter,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                WebtoonBottomControls(
                    onNextChapter = onNextChapter,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
