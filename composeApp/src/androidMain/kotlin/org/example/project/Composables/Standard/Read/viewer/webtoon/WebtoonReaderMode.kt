package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source
import org.example.project.Composables.Standard.Read.viewer.ReaderMode

object WebtoonReaderMode : ReaderMode {
    override val name: String = "Webtoon"

    @Composable
    override fun Content(
        images: List<String>,
        headers: List<Source.Header>,
        modifier: Modifier
    ) {
        WebtoonReader(
            images = images,
            headers = headers,
            modifier = modifier
        )
    }
}