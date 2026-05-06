package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.skeleton.SkeletonBlock

@Composable
fun ChaptersListSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(112.dp)
                    .height(160.dp),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(28.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(18.dp)
                )
            }
        }

        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            shape = RoundedCornerShape(12.dp)
        )

        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(30.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(8) {
                ChapterListItemSkeleton()
            }
        }
    }
}

@Composable
private fun ChapterListItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SkeletonBlock(
            modifier = Modifier.size(10.dp),
            shape = CircleShape
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(18.dp)
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.44f)
                    .height(14.dp)
            )
        }
    }
}
