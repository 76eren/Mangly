package com.eren76.mangly.composables.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

@Composable
fun ShowItemsInLazyGrid(
    historyData: Map<Long, HistoryEntity>,
    sourcesById: Map<UUID, ExtensionMetadata>,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    extensionMetaDataViewModel: ExtensionMetadataViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(
            items = historyData.entries.toList(),
            key = { entry ->
                val timeKey = entry.key
                val historyEntity = entry.value
                historyEntity.id.toString() + timeKey.toString()
            }
        ) { entry ->
            val timeKey = entry.key
            val historyEntity = entry.value

            val sourceMetadata = sourcesById[historyEntity.extensionId]
            val source: Source? = sourceMetadata?.source

            HistoryRow(
                historyEntity = historyEntity,
                lastReadAt = timeKey,
                source = source,
                historyViewModel = historyViewModel,
                onClick = {
                    val encodedUrl = URLEncoder.encode(
                        historyEntity.mangaUrl,
                        Charsets.UTF_8.name()
                    )
                    extensionMetaDataViewModel.setSelectedSource(sourcesById[historyEntity.extensionId]!!) // TODO: not ideal
                    navHostController.navigate("chapters/$encodedUrl")
                }
            )
        }
    }
}

@Composable
fun PaginatedHistoryList(
    historyData: Map<Long, HistoryEntity>,
    sourcesById: Map<UUID, ExtensionMetadata>,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    extensionMetaDataViewModel: ExtensionMetadataViewModel
) {
    val entriesList = remember(historyData) { historyData.entries.toList() }

    var rowHeightPx by remember { mutableIntStateOf(0) }
    var pagerHeightPx by remember { mutableIntStateOf(0) }

    val pageSize = remember(entriesList.size, rowHeightPx, pagerHeightPx) {
        val heightPerItem = rowHeightPx
        val availableHeight = pagerHeightPx
        when {
            entriesList.isEmpty() -> 1
            heightPerItem <= 0 || availableHeight <= 0 -> 1
            else -> (availableHeight / heightPerItem).coerceAtLeast(1)
        }
    }

    val totalPages =
        if (entriesList.isEmpty()) 1 else (entriesList.size + pageSize - 1) / pageSize

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )

    LaunchedEffect(totalPages) {
        val maxPage = (totalPages - 1).coerceAtLeast(0)
        if (pagerState.currentPage > maxPage) {
            pagerState.scrollToPage(maxPage)
        }
    }

    LaunchedEffect(pagerState, pageSize) {
        snapshotFlow { pagerState.currentPage }
            .map { it * pageSize }
            .distinctUntilChanged()
            .collect { }
    }

    var scrubberHeightPx by remember { mutableIntStateOf(0) }
    var scrubberOffsetFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState.currentPage, totalPages) {
        if (totalPages > 1) {
            scrubberOffsetFraction = pagerState.currentPage.toFloat() / (totalPages - 1).toFloat()
        } else {
            scrubberOffsetFraction = 0f
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(12.dp))

            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        pagerHeightPx = size.height
                    }
            ) { page ->
                val startIndex = page * pageSize
                val endIndex = minOf(startIndex + pageSize, entriesList.size)
                val pageItems = if (startIndex < entriesList.size) {
                    entriesList.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }

                if (pageItems.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            items = pageItems,
                            key = { entry ->
                                val timeKey = entry.key
                                val historyEntity = entry.value
                                historyEntity.id.toString() + timeKey.toString()
                            }
                        ) { entry ->
                            val timeKey = entry.key
                            val historyEntity = entry.value

                            val sourceMetadata = sourcesById[historyEntity.extensionId]
                            val source: Source? = sourceMetadata?.source

                            Box(
                                modifier = Modifier.onSizeChanged { size ->
                                    if (rowHeightPx == 0) {
                                        rowHeightPx = size.height
                                    }
                                }
                            ) {
                                HistoryRow(
                                    historyEntity = historyEntity,
                                    lastReadAt = timeKey,
                                    source = source,
                                    historyViewModel = historyViewModel,
                                    onClick = {
                                        val encodedUrl = URLEncoder.encode(
                                            historyEntity.mangaUrl,
                                            Charsets.UTF_8.name()
                                        )
                                        extensionMetaDataViewModel.setSelectedSource(sourcesById[historyEntity.extensionId]!!) // TODO: not ideal
                                        navHostController.navigate("chapters/$encodedUrl")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (totalPages > 1) {
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .onSizeChanged { size ->
                        scrubberHeightPx = size.height
                    }
                    .pointerInput(totalPages, scrubberHeightPx) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            if (scrubberHeightPx <= 0) return@detectVerticalDragGestures

                            val localPosY =
                                change.position.y.coerceIn(0f, scrubberHeightPx.toFloat())
                            val fraction = localPosY / scrubberHeightPx.toFloat()
                            scrubberOffsetFraction = fraction

                            val targetPage = ((totalPages - 1) * fraction)
                                .toInt()
                                .coerceIn(0, totalPages - 1)

                            if (targetPage != pagerState.currentPage) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetPage)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )

                val thumbOffsetDp = with(density) {
                    if (scrubberHeightPx > 0) {
                        (scrubberHeightPx * scrubberOffsetFraction)
                            .coerceIn(0f, scrubberHeightPx.toFloat())
                            .toDp()
                    } else {
                        0.dp
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = thumbOffsetDp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
