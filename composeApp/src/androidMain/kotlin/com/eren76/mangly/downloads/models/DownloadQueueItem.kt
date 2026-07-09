package com.eren76.mangly.downloads.models

import java.util.UUID

data class DownloadQueueItem(
    val workId: UUID,
    val queueKey: String,
    val mangaName: String,
    val chapterName: String,
    val queueIndex: Int,
    val queueTotal: Int,
    val queuedAt: Long,
    val status: DownloadQueueStatus,
    val attempts: Int,
    val message: String?
) {
    val positionLabel: String
        get() = "$queueIndex/$queueTotal"

    val isActive: Boolean
        get() = status == DownloadQueueStatus.Queued ||
            status == DownloadQueueStatus.Running ||
            status == DownloadQueueStatus.Retrying
}

enum class DownloadQueueStatus(val label: String) {
    Queued("Queued"),
    Running("Downloading"),
    Retrying("Retrying"),
    Done("Done"),
    Failed("Failed"),
    Cancelled("Cancelled")
}
