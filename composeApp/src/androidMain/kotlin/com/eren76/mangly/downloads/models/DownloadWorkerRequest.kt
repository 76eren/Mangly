package com.eren76.mangly.downloads.models

data class DownloadWorkerRequest(
    val mangaUrl: String,
    val mangaName: String,
    val mangaSummary: String,
    val chapterUrl: String,
    val chapterName: String,
    val extensionIdValue: String,
    val downloadsDirectory: String,
    val queueIndex: Int,
    val queueTotal: Int,
    val queueBatchId: String,
    val queuedAt: Long,
    val queueKey: String,
    val missingFields: List<String>
)
