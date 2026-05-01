package com.eren76.mangly.composables.screens.readviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eren76.mangly.composables.screens.readviewer.paged.PagedReaderMode
import com.eren76.mangly.composables.screens.readviewer.webtoon.WebtoonReaderMode
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.Source

sealed interface ReaderPageState {
    data object Loading : ReaderPageState
    data class Success(val bytes: ByteArray) : ReaderPageState
    data class Error(val throwable: Throwable? = null) : ReaderPageState
}

data class ReaderPage(
    val index: Int,
    val url: String,
    val state: ReaderPageState = ReaderPageState.Loading,
)

interface ReaderMode {
    val name: String

    @Composable
    fun Content(
        pages: List<ReaderPage>,
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
    const val DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY =
        "mangly_disable_image_saving_on_hold_setting"

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
