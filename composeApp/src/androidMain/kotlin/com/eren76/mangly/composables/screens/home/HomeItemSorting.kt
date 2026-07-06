package com.eren76.mangly.composables.screens.home

import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.entities.HistoryChapterEntity
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.HistoryViewModel

fun sortFavorites(
    favorites: List<FavoritesEntity>,
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel
): List<FavoritesEntity> {
    return sortHomeItems(
        items = favorites,
        sorting = sorting,
        historyViewModel = historyViewModel,
        mapToSortableItem = { favorite ->
            HomeSortableItem(
                item = favorite,
                title = favorite.mangaTitle,
                mangaUrl = favorite.mangaUrl,
                stableId = favorite.id.toString(),
                recentItemTimestamp = timestampOrMin(favorite.created_at)
            )
        }
    )
}

fun sortDownloads(
    downloads: List<DownloadWithChapters>,
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel
): List<DownloadWithChapters> {
    return sortHomeItems(
        items = downloads,
        sorting = sorting,
        historyViewModel = historyViewModel,
        mapToSortableItem = { downloadWithChapters ->
            val download = downloadWithChapters.download

            HomeSortableItem(
                item = downloadWithChapters,
                title = download.mangaName?.takeIf { it.isNotBlank() } ?: download.mangaUrl,
                mangaUrl = download.mangaUrl,
                stableId = download.downloadId.toString(),
                recentItemTimestamp = downloadWithChapters.latestDownloadedAt()
            )
        }
    )
}

private data class HomeSortableItem<T>(
    val item: T,
    val title: String,
    val mangaUrl: String,
    val stableId: String,
    val recentItemTimestamp: Long
)

private fun <T> sortHomeItems(
    items: List<T>,
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel,
    mapToSortableItem: (T) -> HomeSortableItem<T>
): List<T> {
    if (items.isEmpty()) return items

    val sortableItems = items.map(mapToSortableItem)

    return when (sorting) {
        HomeSorting.Alphabetical -> sortableItems.sortedWith(
            compareBy<HomeSortableItem<T>>(
                { it.title.lowercase() },
                { it.mangaUrl },
                { it.stableId }
            )
        )

        HomeSorting.LatestFavorite -> sortableItems.sortedWith(
            compareByDescending<HomeSortableItem<T>> { it.recentItemTimestamp }
                .thenBy { it.title.lowercase() }
                .thenBy { it.mangaUrl }
                .thenBy { it.stableId }
        )

        HomeSorting.LatestRead -> {
            val historyWithChapters = historyViewModel.historyWithChapters.value
            val latestReadByUrl: Map<String, Long> =
                computeLatestReadByMangaUrl(historyWithChapters)

            sortableItems.sortedWith(
                compareByDescending<HomeSortableItem<T>> { sortableItem ->
                    val lastRead = latestReadByUrl[sortableItem.mangaUrl] ?: 0L
                    if (lastRead > 0L) 1 else 0
                }
                    .thenByDescending { sortableItem ->
                        val lastRead = latestReadByUrl[sortableItem.mangaUrl] ?: 0L
                        if (lastRead > 0L) lastRead else Long.MIN_VALUE
                    }
                    .thenByDescending { it.recentItemTimestamp }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.mangaUrl }
                    .thenBy { it.stableId }
            )
        }
    }.map { it.item }
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

private fun DownloadWithChapters.latestDownloadedAt(): Long {
    return chapters.maxOfOrNull { chapter ->
        timestampOrMin(chapter.downloadedAt)
    } ?: Long.MIN_VALUE
}

private fun timestampOrMin(timestamp: Long?): Long {
    val value = timestamp ?: return Long.MIN_VALUE
    return if (value > 0L) value else Long.MIN_VALUE
}
