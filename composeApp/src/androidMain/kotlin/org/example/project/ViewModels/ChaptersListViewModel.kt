package org.example.project.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList

class ChaptersListViewModel : ViewModel() {
    private val chapters = mutableStateOf<List<Source.ChapterValue>?>(null)
    private val image = mutableStateOf<ImageForChaptersList?>(null)
    private val summary = mutableStateOf("")

    fun setChapters(chapterList: List<Source.ChapterValue>?) {
        chapters.value = chapterList
    }

    fun setImage(chapterImage: ImageForChaptersList?) {
        image.value = chapterImage
    }

    fun setSummary(chapterSummary: String) {
        summary.value = chapterSummary
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

    fun clear() {
        chapters.value = null
        image.value = null
        summary.value = ""
    }
}