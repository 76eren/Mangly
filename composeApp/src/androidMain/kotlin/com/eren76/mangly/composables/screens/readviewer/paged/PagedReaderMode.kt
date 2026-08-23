package com.eren76.mangly.composables.screens.readviewer.paged

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderModeType
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
import com.eren76.mangly.composables.shared.read.LongPressImageMenu
import com.eren76.mangly.composables.shared.read.ReadBottomControls
import com.eren76.mangly.composables.shared.read.ReadTopControls
import com.eren76.mangly.composables.shared.read.readerGestures
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val CHAPTER_NAVIGATION_PAGE_COUNT = 1
private const val PAGE_SNAP_DURATION_MILLIS = 220
private val PAGE_SNAP_ANIMATION_SPEC: FiniteAnimationSpec<Float> = tween(
    durationMillis = PAGE_SNAP_DURATION_MILLIS,
    easing = FastOutSlowInEasing
)

class PagedReaderMode(
    modeType: ReaderModeType
) : ReaderMode {
    private val isReverse: Boolean = (modeType == ReaderModeType.REVERSE_PAGED)

    override val name: String = modeType.displayName

    @OptIn(ExperimentalFoundationApi::class)
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
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )
        val doubleTapZoomEnabled = !sharedPreferences.getBoolean(
            ReaderModePrefs.DISABLE_DOUBLE_TAP_ZOOM_SETTING_KEY,
            false
        )

        val pageCount: Int = pages.size + CHAPTER_NAVIGATION_PAGE_COUNT
        val pagerState: PagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { pageCount }
        )
        val pageUrls: List<String> = pages.map(ReaderPage::url)
        val zoomStates: List<PagedZoomState> = remember(pageUrls) {
            List(pages.size) { PagedZoomState() }
        }
        val currentZoomState: PagedZoomState? = zoomStates.getOrNull(pagerState.currentPage)
        val pageNavigationEnabled: Boolean = currentZoomState?.isZoomed != true
        val displayedPageNumber: Int = displayPageNumber(
            pageIndex = pagerState.currentPage,
            imageCount = pages.size
        )

        var showControls: Boolean by remember { mutableStateOf(false) }
        var showLongPressMenu: Boolean by remember { mutableStateOf(false) }

        fun navigateToPage(targetPage: Int) {
            if (targetPage !in 0 until pageCount) return
            if (!pageNavigationEnabled) return

            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = PAGE_SNAP_ANIMATION_SPEC
                )
            }
        }

        LaunchedEffect(pageUrls) {
            pagerState.scrollToPage(0)
        }

        LaunchedEffect(pagerState.settledPage, zoomStates) {
            zoomStates.forEachIndexed { index, state ->
                if (index != pagerState.settledPage) state.reset()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .readerGestures(
                    onControlsTap = {
                        showControls = !showControls
                        showLongPressMenu = false
                    },
                    onDoubleTap = { position ->
                        coroutineScope.launch {
                            currentZoomState?.animateZoomToggleAt(position)
                        }
                        showLongPressMenu = false
                    },
                    onLongPress = {
                        val imageSavingDisabled = sharedPreferences.getBoolean(
                            ReaderModePrefs.DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY,
                            false
                        )

                        if (!imageSavingDisabled && pagerState.currentPage in pages.indices) {
                            showLongPressMenu = true
                        }
                    },
                    isDoubleTapEnabled = doubleTapZoomEnabled
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                reverseLayout = isReverse,
                userScrollEnabled = pageNavigationEnabled,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = PAGE_SNAP_ANIMATION_SPEC
                )
            ) { pageIndex: Int ->
                PagedReaderPage(
                    pageIndex = pageIndex,
                    pages = pages,
                    zoomState = zoomStates.getOrNull(pageIndex),
                    pagerState = pagerState,
                    reverseLayout = isReverse,
                    onRetryPage = onRetryPage,
                    onPreviousChapter = onPreviousChapter,
                    onNextChapter = onNextChapter,
                    chaptersListViewModel = chaptersListViewModel
                )
            }

            PageNumberIndicator(
                currentPage = displayedPageNumber,
                totalPages = pages.size,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showControls) {
                ReadTopControls(
                    currentPage = displayedPageNumber,
                    totalPages = pages.size,
                    chapterTitle = chaptersListViewModel.getSelectedChapterNumber(),
                    onPreviousChapter = onPreviousChapter,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ReadBottomControls(
                    onNextChapter = onNextChapter,
                    currentPage = pagerState.currentPage,
                    totalPages = pageCount,
                    onPageSelected = ::navigateToPage,
                    onPagePreview = pagerState::requestScrollToPage,
                    isPageSliderEnabled = pageNavigationEnabled,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            if (showLongPressMenu && pagerState.currentPage in pages.indices) {
                val selectedPage = pages[pagerState.currentPage]
                val bytes = (selectedPage.state as? ReaderPageState.Success)?.bytes
                if (bytes != null) {
                    LongPressImageMenu(
                        imageBytes = bytes,
                        onDismiss = { showLongPressMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PagedReaderPage(
    pageIndex: Int,
    pages: List<ReaderPage>,
    zoomState: PagedZoomState?,
    pagerState: PagerState,
    reverseLayout: Boolean,
    onRetryPage: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    chaptersListViewModel: ChaptersListViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .bookPageTransition(
                pagerState = pagerState,
                page = pageIndex,
                reverseLayout = reverseLayout
            )
    ) {
        if (pageIndex in pages.indices && zoomState != null) {
            PagedImage(
                page = pages[pageIndex],
                index = pageIndex,
                totalImages = pages.size,
                zoomState = zoomState,
                onRetry = { onRetryPage(pageIndex) }
            )
        } else {
            PagedChapterNavigation(
                onPreviousChapter = onPreviousChapter,
                onNextChapter = onNextChapter,
                chaptersListViewModel = chaptersListViewModel,
                reverseLayout = reverseLayout
            )
        }
    }
}

@Composable
private fun PageNumberIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$currentPage / $totalPages",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun displayPageNumber(pageIndex: Int, imageCount: Int): Int {
    return (pageIndex + 1).coerceAtMost(imageCount)
}

private fun Modifier.bookPageTransition(
    pagerState: PagerState,
    page: Int,
    reverseLayout: Boolean
): Modifier {
    return graphicsLayer {
        val logicalOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).coerceIn(-1f, 1f)
        val visualOffset = if (reverseLayout) -logicalOffset else logicalOffset
        val distance = visualOffset.absoluteValue

        rotationY = visualOffset * 18f
        scaleX = 1f - (0.04f * distance)
        scaleY = 1f - (0.04f * distance)
        alpha = 1f - (0.12f * distance)
        shadowElevation = 12.dp.toPx() * distance
        cameraDistance = 32f * density
        transformOrigin = TransformOrigin(
            pivotFractionX = if (visualOffset > 0f) 0f else 1f,
            pivotFractionY = 0.5f
        )
    }
}
