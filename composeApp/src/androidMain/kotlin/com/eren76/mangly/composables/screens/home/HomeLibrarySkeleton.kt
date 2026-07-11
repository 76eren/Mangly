package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.collection.CollectionGrid
import com.eren76.mangly.composables.shared.collection.GridViewMode
import com.eren76.mangly.composables.shared.collection.rememberCollectionGridState
import com.eren76.mangly.composables.shared.skeleton.SkeletonBlock

private const val SCROLLING_SKELETON_ITEM_COUNT = 6

@Composable
fun HomeLibrarySkeleton(
    viewMode: GridViewMode,
    loadingDescription: String,
    modifier: Modifier = Modifier
) {
    val itemCount = when (viewMode) {
        GridViewMode.Scrolling -> SCROLLING_SKELETON_ITEM_COUNT
        is GridViewMode.Paginated -> viewMode.pageSize
    }
    val placeholders = remember(itemCount) {
        List(itemCount) { index -> index }
    }
    val collectionState = rememberCollectionGridState(
        viewMode = viewMode,
        itemCount = itemCount,
        resetKey = Unit
    )

    Box(
        modifier = modifier.semantics {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    ) {
        CollectionGrid(
            items = placeholders,
            state = collectionState,
            itemKey = { placeholder -> placeholder },
            modifier = Modifier.fillMaxSize(),
            itemContent = { HomeMangaCardSkeleton() }
        )
    }
}

@Composable
private fun HomeMangaCardSkeleton() {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SkeletonBlock(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(11.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}
