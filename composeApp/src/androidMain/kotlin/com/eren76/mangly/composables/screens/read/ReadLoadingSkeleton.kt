package com.eren76.mangly.composables.screens.read

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.skeleton.SkeletonBlock

@Composable
fun ReadLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .height(22.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        repeat(3) { index ->
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(if (index == 1) 0.92f else 1f)
                    .height(if (index == 0) 280.dp else 220.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }

        Box(modifier = Modifier.weight(1f))
    }
}
