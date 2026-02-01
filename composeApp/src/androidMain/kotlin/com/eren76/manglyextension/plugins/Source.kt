package com.eren76.manglyextension.plugins

abstract class Source(val prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    /**
     * Gets the ID  for the extension.
     * Returns a UUID as a String
     * Is also used as the preference key, to create shared preferences based on the extension rather than a global one referencing each extension.
     */
    abstract fun getExtensionId(): String

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
     * TODO: Ideally we'd have a superclass with subclasses for different setting types rather than using 'Any' and nullable fields.
     */
    data class SettingGen(
        var key: String, // The key used to identify the setting
        var description: String, // The text that will show up as the description for the setting
        var defaultValue: Any,

        // Optional fields
        var disables: List<String>? = null, // List of other setting keys that this setting disables when enabled
        var enables: List<String>? = null, // List of other setting keys that this setting enables when enabled
        var content: String? = null, // Additional content or information related to the setting IF needed, not all setting types support/need this
        var uiElement: PreferenceUi? = null // If the UI element is empty it won't be displayed but still be used internally to storage data in the shared preferences
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

    abstract suspend fun search(query: String): List<SearchResult>


    /**
     * Gets the image for the chapter list.
     * @param chaptersUrl The URL the image for the chapter list will get fetched from .
     */
    data class ImageForChaptersList(
        val imageUrl: String,
        val headers: List<Header>
    )

    abstract suspend fun getImageForChaptersList(chaptersUrl: String): ImageForChaptersList

    /**
     * Gets the chapters from the chapter URL.
     * @param targetUrl The URL to fetch the chapters from.
     */
    data class ChapterValue(
        val title: String,
        val url: String,
    )

    /**
     * Gets the chapters from the chapter URL.
     * @param targetUrl The URL to fetch the chapters from.
     */
    abstract suspend fun getChaptersFromChapterUrl(targetUrl: String): List<ChapterValue>

    /**
     * Gets the summary/description which will be shown on the chapter list screen.
     * @param url The URL to fetch the summary from.
     */
    abstract suspend fun getSummary(url: String): String

    /**
     * Gets all images.
     * @param chapterUrl The URL to fetch the images from.
     */
    data class ChapterImages(
        val images: List<String>,
        val headers: List<Header>
    )

    abstract suspend fun getChapterImages(chapterUrl: String): ChapterImages

    /**
     * Gets the manga name from a chapter URL.
     * @param chapterUrl The URL to fetch the manga name from.
     */
    abstract suspend fun getMangaNameFromChapterUrl(chapterUrl: String): String

}