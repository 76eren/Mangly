package com.example.manglyextension.plugins

abstract class Source(prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    data class Header(
        val name: String,
        val value: String
    )

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
        val url: String,
        val headers: List<Header>
    )
    abstract fun search(query: String): List<SearchResult>


    data class ImageForChaptersList(
        val imageUrl: String,
        val headers: List<Header>
    )
    abstract fun getImageForChaptersList(chaptersUrl: String): ImageForChaptersList


    data class ChapterValue(
        val title: String,
        val url: String,
    )
    abstract fun getChaptersFromChapterUrl(targetUrl: String): List<ChapterValue>

    abstract fun getSummary(url: String): String

}