package com.eren76.mangly.composables.shared.image

import coil3.network.NetworkHeaders
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object CoverCache {

    class DownloadedImage(
        val bytes: ByteArray,
        val contentType: String?,
        val finalUrl: String
    )

    suspend fun downloadImage(
        imageUrl: String,
        headers: List<Source.Header>
    ): DownloadedImage? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val requestBuilder = Request.Builder().url(imageUrl)
            for (h in headers) {
                requestBuilder.addHeader(h.name, h.value)
            }

            client.newCall(requestBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bodyBytes = resp.body?.bytes() ?: return@use null
                DownloadedImage(
                    bytes = bodyBytes,
                    contentType = resp.header("Content-Type"),
                    finalUrl = resp.request.url.toString()
                )
            }
        }
    }

    fun buildNetworkHeaders(headers: List<Source.Header>): NetworkHeaders {
        return NetworkHeaders.Builder().apply {
            for (header in headers) {
                this[header.name] = header.value
            }
        }.build()
    }

    fun inferImageExtension(contentType: String?, finalUrl: String?, originalUrl: String?): String {
        return extensionFromContentType(contentType)
            ?: finalUrl?.let { extensionFromUrl(it) }
            ?: originalUrl?.let { extensionFromUrl(it) }
            ?: "jpg"
    }

    private fun extensionFromContentType(contentType: String?): String? {
        val ct = contentType?.substringBefore(';')?.trim()?.lowercase()
        return when (ct) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/avif" -> "avif"
            "image/bmp" -> "bmp"
            "image/svg+xml" -> "svg"
            else -> null
        }
    }

    private fun extensionFromUrl(url: String): String? {
        val withoutQuery = url.substringBefore('?').substringBefore('#')
        val ext = withoutQuery.substringAfterLast('.', missingDelimiterValue = "")
            .trim()
            .lowercase()

        if (ext.isBlank() || ext.length > 5) return null
        if (!ext.all { it.isLetterOrDigit() }) return null

        return ext
    }
}

