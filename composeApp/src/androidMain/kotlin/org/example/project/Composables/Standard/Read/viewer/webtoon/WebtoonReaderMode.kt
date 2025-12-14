package org.example.project.Composables.Standard.Read.viewer.webtoon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source
import org.example.project.Composables.Standard.Read.viewer.ReaderMode
import org.example.project.ViewModels.ChaptersListViewModel

object WebtoonReaderMode : ReaderMode {
    override val name: String = "Webtoon"

    @Composable
    override fun Content(
        images: List<String>,
        headers: List<Source.Header>,
        modifier: Modifier,
        onPreviousChapter: () -> Unit,
        onNextChapter: () -> Unit,
        chaptersListViewModel: ChaptersListViewModel
    ) {
        WebtoonReader(
            images = images,
            headers = headers,
            modifier = modifier,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter,
            chaptersListViewModel = chaptersListViewModel
        )
    }
}