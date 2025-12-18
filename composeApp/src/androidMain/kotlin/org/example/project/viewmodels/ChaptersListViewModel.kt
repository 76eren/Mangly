package org.example.project.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChaptersListViewModel
@Inject constructor() : ViewModel() {

    private val chapters = mutableStateOf<List<Source.ChapterValue>?>(null)
    private val image = mutableStateOf<ImageForChaptersList?>(null)
    private val summary = mutableStateOf("")
    private val name = mutableStateOf("")
    private val scrollPosition = mutableStateOf(0)

    // Horrible way of keeping track of selected chapter number across composable recompositions
    private val selectedChapterNumber = mutableStateOf("")
    private val setSelectedMangaUrl = mutableStateOf("")

    fun setChapters(chapterList: List<Source.ChapterValue>?) {
        chapters.value = chapterList
    }

    fun setImage(chapterImage: ImageForChaptersList?) {
        image.value = chapterImage
    }

    fun setSummary(chapterSummary: String) {
        summary.value = chapterSummary
    }

    fun setName(chapterName: String) {
        name.value = chapterName
    }

    fun getName(): String {
        return name.value
    }

    fun getChapters(): List<Source.ChapterValue> {
        return chapters.value
            ?: emptyList()
    }

    fun getImage(): ImageForChaptersList? {
        return image.value
    }

    fun getSummary(): String {
        return summary.value
    }

    fun setScrollPosition(position: Int) {
        scrollPosition.value = position
    }

    fun getScrollPosition(): Int {
        return scrollPosition.value
    }

    fun clear() {
        chapters.value = null
        image.value = null
        summary.value = ""
        name.value = ""
        scrollPosition.value = 0
    }

    fun setSelectedChapterNumber(chapterNumber: String) {
        selectedChapterNumber.value = chapterNumber
    }

    fun getSelectedChapterNumber(): String {
        return selectedChapterNumber.value
    }

    fun setSelectedMangaUrl(url: String) {
        setSelectedMangaUrl.value = url
    }

    fun getSelectedMangaUrl(): String {
        return setSelectedMangaUrl.value
    }
}