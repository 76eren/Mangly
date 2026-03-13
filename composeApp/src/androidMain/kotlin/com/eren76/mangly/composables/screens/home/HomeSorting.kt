package com.eren76.mangly.composables.screens.home

// TODO: This should be moved somewhere else but I am not sure where
enum class HomeSorting(val prefValue: String, val label: String) {
    LatestRead("latest_read", "Latest chapter read"),
    LatestFavorite("latest_favorite", "Latest added to favorites"),
    Alphabetical("alphabetical", "By alphabet");

    companion object {
        const val DEFAULT_PREF_VALUE = "latest_read"

        fun fromPrefValue(value: String?): HomeSorting = when (value) {
            LatestFavorite.prefValue -> LatestFavorite
            Alphabetical.prefValue -> Alphabetical
            else -> LatestRead
        }
    }
}

