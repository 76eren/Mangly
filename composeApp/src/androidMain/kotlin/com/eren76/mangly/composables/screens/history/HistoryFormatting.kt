package com.eren76.mangly.composables.screens.history

import com.eren76.mangly.rooms.entities.HistoryChapterEntity
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.mangly.viewmodels.HistoryViewModel
import java.text.DateFormat
import java.util.Date
import java.util.SortedMap
import java.util.TreeMap

fun getHistoryDataNewestToOldest(historyViewModel: HistoryViewModel): SortedMap<Long, HistoryEntity> {
    val historyEntitiesNewestToOldestReadState: SortedMap<Long, HistoryEntity> =
        TreeMap(compareByDescending { it })
    val historyEntityByTime = HashMap<Long, HistoryEntity>()

    for (chapter: HistoryWithReadChapters in historyViewModel.historyWithChapters.value) {
        var latest = 0L
        for (readChapter: HistoryChapterEntity in chapter.readChapters) {
            val readAt = readChapter.readAt ?: continue
            if (readAt > latest) {
                latest = readAt
            }
        }
        if (latest > 0L) {
            historyEntityByTime[latest] = chapter.history
        }
    }

    for ((key, historyEntity) in historyEntityByTime) {
        historyEntitiesNewestToOldestReadState[key] = historyEntity
    }

    return historyEntitiesNewestToOldestReadState
}

fun formatLastRead(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestamp))
}

