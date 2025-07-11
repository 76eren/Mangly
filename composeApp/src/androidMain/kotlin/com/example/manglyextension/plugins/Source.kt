// Package matches location in extension project

package com.example.manglyextension.plugins

abstract class Source(prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    abstract fun getExtensionName(): String

    data class SettingGen(
        var key: String,
        var defaultValue: Any,
        var uiElement: PreferenceUi? = null
    )
    abstract fun generateSettings(): List<SettingGen>


    data class SearchResult(
        val title: String,
        val imageUrl: String,
        val url: String
    )
    abstract fun search(query: String): List<SearchResult>

    abstract fun getReferer(): String
    abstract fun getUserAgent(): String

    data class ImageForChaptersList(
        val imageUrl: String,
        val referer: String?,
    )
    abstract fun getImageForChaptersList(chaptersUrl: String): ImageForChaptersList


    data class ChapterValue(
        val title: String,
        val url: String,
    )
    abstract fun getChaptersFromChapterUrl(targetUrl: String): List<ChapterValue>
}