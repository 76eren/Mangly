package com.eren76.mangly.composables.screens.home

enum class HomeMode(
    val title: String,
    val loadingText: String,
    val emptyText: String,
    val emptyFilteredText: String
) {
    Favorites(
        title = "Favorites",
        loadingText = "Loading favorites...",
        emptyText = "No favorites yet.",
        emptyFilteredText = "No favorites for this source."
    ),
    Downloads(
        title = "Downloads",
        loadingText = "Loading downloads...",
        emptyText = "No downloads yet.",
        emptyFilteredText = "No downloads for this source."
    );

    companion object {
        fun fromShowDownloads(showDownloads: Boolean): HomeMode {
            return if (showDownloads) Downloads else Favorites
        }
    }
}
