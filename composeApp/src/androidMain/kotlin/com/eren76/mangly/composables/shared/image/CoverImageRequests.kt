package com.eren76.mangly.composables.shared.image

import android.content.Context
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.manglyextension.plugins.Source
import java.io.File

object CoverImageRequests {

    fun local(context: Context, file: File?): ImageRequest? {
        return file?.let {
            ImageRequest.Builder(context)
                .data(it)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .build()
        }
    }

    fun remote(
        context: Context,
        imageForList: Source.ImageForChaptersList?,
        crossfade: Boolean = true
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageForList?.imageUrl)
            .apply {
                val hdrs = imageForList?.headers
                if (!hdrs.isNullOrEmpty()) httpHeaders(CoverCache.buildNetworkHeaders(hdrs))
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfade)
            .build()
    }
}

