package com.eren76.mangly.composables.screens.readviewer.webtoon

import androidx.paging.PagingSource
import androidx.paging.PagingState

class WebtoonImagePagingSource(
    private val images: List<String>
) : PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        return try {
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, images.size)

            if (start >= images.size) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }

            LoadResult.Page(
                data = images.subList(start, end),
                prevKey = if (start == 0) null else maxOf(0, start - params.loadSize),
                nextKey = if (end < images.size) end else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return state.anchorPosition
    }
}