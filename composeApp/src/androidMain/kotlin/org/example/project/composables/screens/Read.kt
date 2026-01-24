package org.example.project.composables.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.Constants
import org.example.project.composables.screens.readviewer.ReaderMode
import org.example.project.composables.screens.readviewer.ReaderModePrefs
import org.example.project.composables.screens.readviewer.ReaderModeType
import org.example.project.composables.screens.readviewer.createReaderMode
import org.example.project.composables.screens.readviewer.getReaderModeTypeFromPref
import org.example.project.viewmodels.ChaptersListViewModel
import org.example.project.viewmodels.ExtensionMetadataViewModel
import org.example.project.viewmodels.HistoryViewModel
import java.util.UUID

@Composable
fun Read(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    historyViewModel: HistoryViewModel
) {
    var url by remember(targetUrl) { mutableStateOf(targetUrl) }
    var chapterImages by remember { mutableStateOf<Source.ChapterImages?>(null) }

    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value
    if (metadata == null) {
        Text(
            text = "Something went wrong while loading the chapter images.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )
    }

    val modeValue = prefs.getString(
        ReaderModePrefs.KEY_READER_MODE,
        ReaderModePrefs.DEFAULT_READER_MODE_VALUE
    )
    val modeType: ReaderModeType = getReaderModeTypeFromPref(modeValue)
    val readerMode: ReaderMode = createReaderMode(modeType)

    LaunchedEffect(url, metadata) {
        chapterImages = withContext(Dispatchers.IO) {
            getChapterImages(url, metadata)
        }
    }

    if (chapterImages != null && chapterImages!!.images.isNotEmpty()) {
        readerMode.Content(
            images = chapterImages!!.images,
            headers = chapterImages!!.headers,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f),
            onNextChapter = {
                val chapters = chaptersListViewModel.getChapters()
                val currentIndex = chapters.indexOfFirst { it.url == url }
                if (currentIndex >= 0 && currentIndex + 1 < chapters.size) {
                    url = chapters[currentIndex + 1].url
                    chaptersListViewModel.setSelectedChapterNumber(chapters[currentIndex + 1].title)
                    Log.d("lol", "onNextChapter: $url")

                    historyViewModel.ensureHistoryAndAddChapter(
                        mangaUrl = chaptersListViewModel.getSelectedMangaUrl(),
                        mangaName = chaptersListViewModel.getName(),
                        extensionId = UUID.fromString(metadata.source.getExtensionId()),
                        chapterUrl = url
                    )
                } else {
                    Toast.makeText(context, "No next chapter available", Toast.LENGTH_SHORT).show()
                }
            },
            onPreviousChapter = {
                val chapters = chaptersListViewModel.getChapters()
                val currentIndex = chapters.indexOfFirst { it.url == url }
                if (currentIndex > 0) {
                    url = chapters[currentIndex - 1].url
                    chaptersListViewModel.setSelectedChapterNumber(chapters[currentIndex - 1].title)
                    Log.d("lol", "onPreviousChapter: $url")

                    historyViewModel.ensureHistoryAndAddChapter(
                        mangaUrl = chaptersListViewModel.getSelectedMangaUrl(),
                        mangaName = chaptersListViewModel.getName(),
                        extensionId = UUID.fromString(metadata.source.getExtensionId()),
                        chapterUrl = url
                    )
                } else {
                    Toast.makeText(context, "No previous chapter available", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            chaptersListViewModel = chaptersListViewModel
        )

        historyViewModel.ensureHistoryAndAddChapter(
            mangaUrl = chaptersListViewModel.getSelectedMangaUrl(),
            mangaName = chaptersListViewModel.getName(),
            extensionId = UUID.fromString(metadata.source.getExtensionId()),
            chapterUrl = url
        )

    } else {
        Text(
            text = "Loading images or no images available...",
            modifier = Modifier.padding(16.dp)
        )
    }
}


suspend fun getChapterImages(url: String, metadata: ExtensionMetadata): Source.ChapterImages {
    try {
        val data = metadata.source.getChapterImages(url)
        return data
    } catch (e: Exception) {
        return Source.ChapterImages(emptyList(), emptyList())
    }
}
