package org.example.project.composables.screens.readviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.manglyextension.plugins.Source
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
    const val PREFS_NAME: String = "mangly_settings"
    const val KEY_READER_MODE: String = "readmode"
    const val DEFAULT_READER_MODE_VALUE: String = "webtoon"
}

enum class ReaderModeType(val prefValue: String) {
    WEBTOON("webtoon");
}

fun getReaderModeTypeFromPref(value: String?): ReaderModeType {
    return if (value == ReaderModeType.WEBTOON.prefValue) {
        ReaderModeType.WEBTOON
    } else {
        ReaderModeType.WEBTOON
    }
}

fun createReaderMode(type: ReaderModeType): ReaderMode {
    return when (type) {
        ReaderModeType.WEBTOON -> WebtoonReaderMode
    }
}
