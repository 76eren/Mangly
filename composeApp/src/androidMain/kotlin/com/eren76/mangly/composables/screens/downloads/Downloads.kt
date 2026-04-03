package com.eren76.mangly.composables.screens.downloads

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.eren76.mangly.viewmodels.DownloadsViewModel

@Composable
fun Downloads(downloadsViewModel: DownloadsViewModel) {
    Text("Downloads: ${downloadsViewModel.downloads.value.size}")
}