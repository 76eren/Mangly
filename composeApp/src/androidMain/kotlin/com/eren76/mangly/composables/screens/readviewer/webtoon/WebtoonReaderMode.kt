package com.eren76.mangly.composables.screens.readviewer.webtoon

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.network.NetworkHeaders
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
import com.eren76.mangly.composables.shared.read.LongPressImageMenu
import com.eren76.mangly.composables.shared.read.ReadBottomControls
import com.eren76.mangly.composables.shared.read.ReadTopControls
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.launch

object WebtoonReaderMode : ReaderMode {
    override val name: String = "Webtoon"

    @Composable
    override fun Content(
        pages: List<ReaderPage>,
        headers: List<Source.Header>,
        modifier: Modifier,
        onPreviousChapter: () -> Unit,
        onNextChapter: () -> Unit,
        chaptersListViewModel: ChaptersListViewModel
    ) {

        var showControls by remember { mutableStateOf(false) }
        var showLongPressMenu by remember { mutableStateOf(false) }
        var selectedLongPressImageUrl by remember(pages) { mutableStateOf<String?>(null) }
        var selectedLongPressImageBytes by remember(pages) { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()


        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )

        val lazyListState = rememberLazyListState()

        remember(headers) {
            NetworkHeaders.Builder().apply {
                for (header in headers) {
                    this[header.name] = header.value
                }
            }.build()
        }


        val currentPage by remember {
            derivedStateOf {
                val firstVisibleItem = lazyListState.firstVisibleItemIndex

                // Account for header item
                if (firstVisibleItem > 0) firstVisibleItem else 1
            }
        }

        // Reset scroll position when images change (new chapter)
        LaunchedEffect(pages) {
            ImageHeightCache.clear()
            lazyListState.scrollToItem(0)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                        count = pages.size,
                        key = { index -> "image_$index" }
                    ) { index ->
                        val page = pages[index]

                        WebtoonImage(
                            page = page,
                            index = index,
                            totalImages = pages.size,
                            onTap = {
                                showControls = !showControls
                                showLongPressMenu = false
                            },
                            onLongPress = {
                                if (!sharedPreferences.getBoolean(
                                        ReaderModePrefs.DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY,
                                        false
                                    )
                                ) {
                                    // Freeze the selection at the time of long-press.
                                    // Using index later can point to a different item if the list
                                    // changes or if recomposition reorders/reuses slots.
                                    selectedLongPressImageUrl = page.url
                                    selectedLongPressImageBytes =
                                        (page.state as? ReaderPageState.Success)?.bytes
                                    showLongPressMenu = true
                                    showControls = false
                                }
                            }
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
                        totalPages = pages.size,
                        chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                        onPreviousChapter = onPreviousChapter,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    ReadBottomControls(
                        onNextChapter = onNextChapter,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        currentPage = currentPage,
                        totalPages = pages.size,
                        onGoToPage = { page ->
                            coroutineScope.launch {
                                lazyListState.scrollToItem(page)
                            }
                        }

                    )
                }

                selectedLongPressImageUrl
                    ?.takeIf { showLongPressMenu }
                    ?.let { imageUrl ->
                        val bytes = selectedLongPressImageBytes
                        if (bytes != null) {
                            LongPressImageMenu(
                                imageBytes = bytes,
                                onDismiss = {
                                    showLongPressMenu = false
                                    selectedLongPressImageUrl = null
                                    selectedLongPressImageBytes = null
                                }
                            )
                        }
                    }
            }
        }

    }
}

