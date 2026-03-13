package com.eren76.mangly.composables.screens.home

import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.entities.HistoryChapterEntity
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.mangly.viewmodels.HistoryViewModel

fun sortFavorites(
    favorites: List<FavoritesEntity>,
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel
): List<FavoritesEntity> {
    if (favorites.isEmpty()) return favorites

    return when (sorting) {
        HomeSorting.Alphabetical -> favorites.sortedWith(
            compareBy(
                { it.mangaTitle.lowercase() },
                { it.mangaUrl },
                { it.id.toString() }
            ))

        HomeSorting.LatestFavorite -> favorites.sortedWith(
            compareByDescending<FavoritesEntity> { favorite ->
                val ts = favorite.created_at
                if (ts > 0L) ts else Long.MIN_VALUE
            }.thenBy { it.mangaTitle.lowercase() }
                .thenBy { it.mangaUrl }
                .thenBy { it.id.toString() }
        )

        HomeSorting.LatestRead -> {
            val historyWithChapters = historyViewModel.historyWithChapters.value
            val latestReadByUrl: Map<String, Long> =
                computeLatestReadByMangaUrl(historyWithChapters)

            favorites.sortedWith(
                compareByDescending<FavoritesEntity> { favorite ->
                    val lastRead = latestReadByUrl[favorite.mangaUrl] ?: 0L
                    val hasHistory = lastRead > 0L
                    if (hasHistory) 1 else 0
                }
                    .thenByDescending { favorite ->
                        val lastRead = latestReadByUrl[favorite.mangaUrl] ?: 0L
                        if (lastRead > 0L) lastRead else Long.MIN_VALUE
                    }
                    .thenByDescending { favorite ->
                        val ts = favorite.created_at
                        if (ts > 0L) ts else Long.MIN_VALUE
                    }
                    .thenBy { it.mangaTitle.lowercase() }
                    .thenBy { it.mangaUrl }
                    .thenBy { it.id.toString() }
            )
        }
    }
}

fun computeLatestReadByMangaUrl(
    historyWithChapters: List<HistoryWithReadChapters>
): Map<String, Long> {
    val latestByUrl = mutableMapOf<String, Long>()

    for (entry: HistoryWithReadChapters in historyWithChapters) {
        val history = entry.history
        var latest = 0L
        for (readChapter: HistoryChapterEntity in entry.readChapters) {
            val readAt = readChapter.readAt ?: continue
            if (readAt > latest) {
                latest = readAt
            }
        }
        if (latest <= 0L) continue

        val url = history.mangaUrl
        val current = latestByUrl[url]
        if (current == null || latest > current) {
            latestByUrl[url] = latest
        }
    }

    return latestByUrl
}

