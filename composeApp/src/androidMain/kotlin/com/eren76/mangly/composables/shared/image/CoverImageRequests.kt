package com.eren76.mangly.composables.shared.image

import android.content.Context
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.manglyextension.plugins.Source
import java.io.File

object CoverImageRequests {

    fun local(context: Context, file: File?): ImageRequest? {
        return file?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(false)
                .build()
        }
    }

    fun remote(
        context: Context,
        imageForList: Source.ImageForChaptersList?,
        networkHeaders: NetworkHeaders,
        crossfade: Boolean
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageForList?.imageUrl)
            .apply {
                val hdrs = imageForList?.headers
                if (!hdrs.isNullOrEmpty()) httpHeaders(networkHeaders)
            }
            .crossfade(crossfade)
            .build()
    }
}

