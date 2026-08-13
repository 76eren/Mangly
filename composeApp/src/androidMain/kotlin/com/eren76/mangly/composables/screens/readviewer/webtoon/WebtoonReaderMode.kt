package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
import com.eren76.mangly.composables.shared.read.LongPressImageMenu
import com.eren76.mangly.composables.shared.read.ReadBottomControls
import com.eren76.mangly.composables.shared.read.ReadTopControls
import com.eren76.mangly.composables.shared.read.readerGestures
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source

private const val HEADER_ITEM_COUNT = 1

object WebtoonReaderMode : ReaderMode {
    override val name: String = "Webtoon"

    @Composable
    override fun Content(
        pages: List<ReaderPage>,
        headers: List<Source.Header>,
        modifier: Modifier,
        onRetryPage: (Int) -> Unit,
        onPreviousChapter: () -> Unit,
        onNextChapter: () -> Unit,
        chaptersListViewModel: ChaptersListViewModel
    ) {
        var showControls: Boolean by remember { mutableStateOf(false) }
        var selectedLongPressImageBytes: ByteArray? by remember(pages) {
            mutableStateOf<ByteArray?>(
                null
            )
        }
        val context: Context = LocalContext.current

        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )

        val lazyListState = rememberLazyListState()
        val zoomState = remember { WebtoonZoomState() }

        val lastPageIndex = pages.lastIndex.coerceAtLeast(0)
        val currentPageIndex: Int by remember(lazyListState, lastPageIndex) {
            derivedStateOf {
                (lazyListState.firstVisibleItemIndex - HEADER_ITEM_COUNT)
                    .coerceIn(0, lastPageIndex)
            }
        }

        fun requestPage(pageIndex: Int) {
            if (pageIndex !in pages.indices) {
                return
            }

            lazyListState.requestScrollToItem(pageIndex + HEADER_ITEM_COUNT)
        }

        // Reset scroll position when images change (new chapter)
        LaunchedEffect(pages) {
            ImageHeightCache.clear()
            zoomState.reset()
            lazyListState.scrollToItem(0)
        }

        fun showLongPressMenuAt(position: Offset) {
            val imageSavingDisabled: Boolean = sharedPreferences.getBoolean(
                ReaderModePrefs.DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY,
                false
            )
            if (imageSavingDisabled) {
                return
            }

            val contentPosition = zoomState.viewportToContent(position)
            val pressedItem: LazyListItemInfo =
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    contentPosition.y >= item.offset &&
                            contentPosition.y < item.offset + item.size
                } ?: return
            val page: ReaderPage = pages.getOrNull(pressedItem.index - 1) ?: return

            selectedLongPressImageBytes =
                (page.state as? ReaderPageState.Success)?.bytes
            showControls = false
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .readerGestures(
                    onSideTap = {
                        showControls = !showControls
                        selectedLongPressImageBytes = null
                    },
                    onCenterDoubleTap = zoomState::toggleZoomAt,
                    onLongPress = ::showLongPressMenuAt,
                    isSideTapEnabled = { !lazyListState.isScrollInProgress }
                )
        ) {
            ZoomableReaderContainer(
                zoomState = zoomState,
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
                        count = pages.size,
                        key = { index -> "image_$index" }
                    ) { index ->
                        val page = pages[index]

                        WebtoonImage(
                            page = page,
                            index = index,
                            totalImages = pages.size,
                            onRetry = { onRetryPage(index) }
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
            }

            if (showControls) {
                ReadTopControls(
                    currentPage = currentPageIndex + 1,
                    totalPages = pages.size,
                    chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                    onPreviousChapter = onPreviousChapter,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ReadBottomControls(
                    onNextChapter = onNextChapter,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    currentPage = currentPageIndex,
                    totalPages = pages.size,
                    onPageSelected = ::requestPage,
                    onPagePreview = ::requestPage
                )
            }

            selectedLongPressImageBytes?.let { bytes ->
                LongPressImageMenu(
                    imageBytes = bytes,
                    onDismiss = { selectedLongPressImageBytes = null }
                )
            }
        }
    }
}
