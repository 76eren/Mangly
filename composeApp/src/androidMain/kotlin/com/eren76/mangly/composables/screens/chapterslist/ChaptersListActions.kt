package com.eren76.mangly.composables.screens.chapterslist

import androidx.navigation.NavHostController
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.manglyextension.plugins.LoadedSource
import com.eren76.manglyextension.plugins.Source
import com.eren76.manglyextension.plugins.Source.ImageForChaptersList
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

suspend fun fetchChapterImage(source: LoadedSource, url: String): ImageForChaptersList {
    return source.source.getImageForChaptersList(url)
}

suspend fun fetchChapterList(source: LoadedSource, url: String): List<Source.ChapterValue> {
    return source.source.getChaptersFromChapterUrl(url)
}

suspend fun fetchSummary(source: LoadedSource, url: String): String {
    return source.source.getSummary(url)
}

suspend fun fetchMangaTitle(source: LoadedSource, url: String): String {
    return source.source.getMangaNameFromChapterUrl(url)
}

fun onChapterClick(
    navHostController: NavHostController,
    mangaUrl: String,
    chaptersListViewModel: ChaptersListViewModel,
    scrollPosition: Int,
    chapterTitle: String,
    chapterUrl: String,
    isDownload: Boolean
) {
    // Probably not the best way of sharing states between composables
    chaptersListViewModel.setScrollPosition(scrollPosition)
    chaptersListViewModel.setSelectedChapterNumber(chapterTitle)
    chaptersListViewModel.setSelectedMangaUrl(chapterUrl)
    val encodedUrl = URLEncoder.encode(mangaUrl, StandardCharsets.UTF_8.toString())

    if (!isDownload) {
        navHostController.navigate("read/${encodedUrl}")
    } else {
        navHostController.navigate("read/${encodedUrl}/downloads")
    }
}

