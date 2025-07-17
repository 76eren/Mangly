package com.example.manglyextension.plugins

abstract class Source(prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    /**
     * This class represents a header that can be used in HTTP requests.
     */
    data class Header(
        val name: String,
        val value: String
    )

    /**
     * Gets the extension name.
     */
    abstract fun getExtensionName(): String

    /**
     * Gets the settings needed by the the extension
     * A uiElement may be provided to indicate how the setting should be displayed in the UI, if empty it won't be displayed.
     */
    data class SettingGen(
        var key: String,
        var defaultValue: Any,
        var uiElement: PreferenceUi? = null
    )
    abstract fun generateSettings(): List<SettingGen>


    /**
     * Gets the search results.
     * @param query The search query.
     */
    data class SearchResult(
        val title: String,
        val imageUrl: String,
        val url: String,
        val headers: List<Header>
    )
    abstract fun search(query: String): List<SearchResult>


    /**
     * Gets the image for the chapter list.
     * @param chaptersUrl The URL the image for the chapter list will get fetched from .
     */
    data class ImageForChaptersList(
        val imageUrl: String,
        val headers: List<Header>
    )
    abstract fun getImageForChaptersList(chaptersUrl: String): ImageForChaptersList

    /**
     * Gets the chapters from the chapter URL.
     * @param targetUrl The URL to fetch the chapters from.
     */
    data class ChapterValue(
        val title: String,
        val url: String,
    )
    abstract fun getChaptersFromChapterUrl(targetUrl: String): List<ChapterValue>

    /**
     * Gets the summary/description which will be shown on the chapter list screen.
     * @param url The URL to fetch the summary from.
     */
    abstract fun getSummary(url: String): String

    /**
     * Gets all images.
     * @param chapterUrl The URL to fetch the images from.
     */
    data class ChapterImages(
        val images: List<String>,
        val headers: List<Header>
    )
    abstract fun getChapterImages(chapterUrl: String): ChapterImages

}