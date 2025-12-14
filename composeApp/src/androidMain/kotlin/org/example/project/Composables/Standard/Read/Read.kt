package org.example.project.Composables.Standard.Read

import android.util.Log
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
import org.example.project.Composables.Standard.Read.viewer.ReaderMode
import org.example.project.Composables.Standard.Read.viewer.ReaderModePrefs
import org.example.project.Composables.Standard.Read.viewer.ReaderModeType
import org.example.project.Composables.Standard.Read.viewer.createReaderMode
import org.example.project.Composables.Standard.Read.viewer.getReaderModeTypeFromPref
import org.example.project.ViewModels.ChaptersListViewModel
import org.example.project.ViewModels.ExtensionMetadataViewModel

@Composable
fun Read(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel
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
            ReaderModePrefs.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
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
                }
                Log.d("lol", "onNextChapter: $url")
            },
            onPreviousChapter = {
                val chapters = chaptersListViewModel.getChapters()
                val currentIndex = chapters.indexOfFirst { it.url == url }
                if (currentIndex > 0) {
                    url = chapters[currentIndex - 1].url
                }
                Log.d("lol", "onPreviousChapter: $url")
            }
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