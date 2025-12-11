package org.example.project.Composables.Standard.Read.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source

interface ReaderMode {
    val name: String

    @Composable
    fun Content(
        images: List<String>,
        headers: List<Source.Header>,
        modifier: Modifier = Modifier
    )
}