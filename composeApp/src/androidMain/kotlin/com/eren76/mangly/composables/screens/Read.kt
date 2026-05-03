package com.eren76.mangly.composables.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderMode
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderModeType
import com.eren76.mangly.composables.screens.readviewer.ReaderPage
import com.eren76.mangly.composables.screens.readviewer.ReaderPageState
import com.eren76.mangly.composables.screens.readviewer.createReaderMode
import com.eren76.mangly.composables.screens.readviewer.getReaderModeTypeFromPref
import com.eren76.mangly.composables.screens.readviewer.loadReaderPagesIncrementally
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun Read(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    historyViewModel: HistoryViewModel,
    downloadsViewModel: DownloadsViewModel,
    download: Boolean
) {
    var url by remember(targetUrl) { mutableStateOf(targetUrl) }
    var chapterImages by remember { mutableStateOf<Source.ChapterImages?>(null) }
    val pages = remember(url) { mutableStateListOf<ReaderPage>() }

    val relatedDownload: DownloadWithChapters? = remember(
        downloadsViewModel.downloads.value,
        chaptersListViewModel.getSelectedMangaUrl(),
        download
    ) {
        if (!download) null
        else downloadsViewModel.downloads.value.find { it.download.mangaUrl == chaptersListViewModel.getSelectedMangaUrl() }
    }

    val resolvedExtensionId: UUID? =
        extensionMetadataViewModel.selectedSingleSource.value?.source?.getExtensionId()
            ?.let { UUID.fromString(it) }

    fun addToHistoryIfPossible(chapterUrl: String) {
        val extId = resolvedExtensionId ?: return
        historyViewModel.ensureHistoryAndAddChapter(
            mangaUrl = chaptersListViewModel.getSelectedMangaUrl(),
            mangaName = chaptersListViewModel.getName(),
            extensionId = extId,
            chapterUrl = chapterUrl
        )
    }

    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value
    if (!download && metadata == null) {
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

    // Online mode: fetch chapter images from the extension source.
    LaunchedEffect(url, metadata, download) {
        if (download) return@LaunchedEffect
        val safeMetadata = metadata ?: return@LaunchedEffect

        chapterImages = withContext(Dispatchers.IO) {
            getChapterImages(url, safeMetadata)
        }
    }

    // Online mode: load pages through Coi
    LaunchedEffect(chapterImages, download) {
        if (download) return@LaunchedEffect

        val imgs: List<String> = chapterImages?.images.orEmpty()
        if (imgs.isEmpty()) return@LaunchedEffect

        pages.clear()
        pages.addAll(imgs.mapIndexed { index, imageUrl ->
            ReaderPage(index = index, url = imageUrl)
        })

        val headers = NetworkHeaders.Builder().apply {
            for (header in chapterImages?.headers.orEmpty()) {
                this[header.name] = header.value
            }
        }.build()

        val imageLoader = ImageLoader(context)
        loadReaderPagesIncrementally(
            context = context,
            imageLoader = imageLoader,
            pages = pages,
            headers = headers
        )
    }

    // Download mode: list local image files and load bytes from disk.
    LaunchedEffect(url, relatedDownload, download) {
        if (!download) return@LaunchedEffect

        val chapter: DownloadedChapterEntity? = relatedDownload
            ?.chapters
            ?.firstOrNull { it.isFullyDownloaded && it.chapterUrl == url }

        val chapterPath: String? = chapter?.filePath
        if (chapterPath.isNullOrBlank()) {
            pages.clear()
            return@LaunchedEffect
        }

        val imageFiles: List<File> = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, chapterPath)
            val unsortedImageFiles: List<File> = dir.listFiles()?.toList().orEmpty()

            unsortedImageFiles
                .filter {
                    it.isFile && it.extension.lowercase() in setOf(
                        "jpg",
                        "jpeg",
                        "png",
                        "webp",
                        "gif"
                    )
                }
                .sortedWith(
                    compareBy<File> {
                        it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
                    }.thenBy { it.name }
                )
        }


        pages.clear()
        pages.addAll(imageFiles.mapIndexed { index, file ->
            ReaderPage(index = index, url = file.absolutePath)
        })

        val loadedPages = withContext(Dispatchers.IO) {
            pages.map { page ->
                val bytes = runCatching { File(page.url).readBytes() }.getOrNull()
                if (bytes == null) page.copy(state = ReaderPageState.Error())
                else page.copy(state = ReaderPageState.Success(bytes))
            }
        }

        pages.clear()
        pages.addAll(loadedPages)
    }

    val canRender =
        if (download) pages.isNotEmpty() else (chapterImages != null && chapterImages!!.images.isNotEmpty())

    if (canRender) {
        readerMode.Content(
            pages = pages,
            headers = if (download) emptyList() else chapterImages!!.headers,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f),
            onNextChapter = {
                val chapters = chaptersListViewModel.getChapters()
                val currentIndex = chapters.indexOfFirst { it.url == url }
                if (currentIndex >= 0 && currentIndex + 1 < chapters.size) {
                    url = chapters[currentIndex + 1].url
                    chaptersListViewModel.setSelectedChapterNumber(chapters[currentIndex + 1].title)
                    addToHistoryIfPossible(url)
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

                    addToHistoryIfPossible(url)
                } else {
                    Toast.makeText(context, "No previous chapter available", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            chaptersListViewModel = chaptersListViewModel
        )

        // Initial/refresh history update.
        addToHistoryIfPossible(url)

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
    } catch (_: Exception) {
        return Source.ChapterImages(emptyList(), emptyList())
    }
}
