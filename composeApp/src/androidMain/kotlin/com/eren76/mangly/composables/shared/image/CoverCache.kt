package com.eren76.mangly.composables.shared.image

import coil3.network.NetworkHeaders
import com.eren76.mangly.SharedImageHttpClient
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
        val requestBuilder = Request.Builder().url(imageUrl)
        for (header in headers) {
            requestBuilder.addHeader(header.name, header.value)
        }

        return suspendCancellableCoroutine { continuation ->
            val call = SharedImageHttpClient.instance.newCall(requestBuilder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, error: IOException) {
                        continuation.resumeWithException(error)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val downloadedImage = response.use { result ->
                                if (!result.isSuccessful) return@use null
                                val bodyBytes = result.body?.bytes() ?: return@use null
                                DownloadedImage(
                                    bytes = bodyBytes,
                                    contentType = result.header("Content-Type"),
                                    finalUrl = result.request.url.toString()
                                )
                            }
                            continuation.resume(downloadedImage)
                        } catch (error: Exception) {
                            continuation.resumeWithException(error)
                        }
                    }
                }
            )
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

