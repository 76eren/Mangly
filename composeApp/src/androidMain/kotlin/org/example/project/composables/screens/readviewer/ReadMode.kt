package org.example.project.composables.screens.readviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source
import org.example.project.composables.screens.readviewer.paged.PagedReaderMode
import org.example.project.composables.screens.readviewer.webtoon.WebtoonReaderMode
import org.example.project.viewmodels.ChaptersListViewModel

interface ReaderMode {
    val name: String

    @Composable
    fun Content(
        images: List<String>,
        headers: List<Source.Header>,
        modifier: Modifier,
        onPreviousChapter: () -> Unit,
        onNextChapter: () -> Unit,
        chaptersListViewModel: ChaptersListViewModel
    )
}

// Preference keys and reader-mode mapping used by Settings and Read
object ReaderModePrefs {
    const val KEY_READER_MODE: String = "readmode"
    const val DEFAULT_READER_MODE_VALUE: String = "webtoon"

    // Settings that are shared between reading modes can be added here
    const val IMAGE_PRELOAD_AMOUNT: String =
        "read_image_preload_amount" // Currently this is only used by webtoon mode, but I plan to re-use this for the other reading modes in the future

}

enum class ReaderModeType(val prefValue: String, val displayName: String) {
    WEBTOON("webtoon", "Webtoon"),
    PAGED("paged", "Paged");
}

fun getReaderModeTypeFromPref(value: String?): ReaderModeType {
    return when (value) {
        ReaderModeType.WEBTOON.prefValue -> ReaderModeType.WEBTOON
        ReaderModeType.PAGED.prefValue -> ReaderModeType.PAGED
        else -> ReaderModeType.WEBTOON
    }
}

fun createReaderMode(type: ReaderModeType): ReaderMode {
    return when (type) {
        ReaderModeType.WEBTOON -> WebtoonReaderMode
        ReaderModeType.PAGED -> PagedReaderMode
    }
}
