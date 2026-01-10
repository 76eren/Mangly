package org.example.project.composables.screens.readviewer.webtoon

import androidx.paging.PagingSource
import androidx.paging.PagingState

class WebtoonImagePagingSource(
    private val images: List<String>
) : PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, images.size)

            if (startIndex >= images.size) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page > 0) page - 1 else null,
                    nextKey = null
                )
            }

            val pageImages = images.subList(startIndex, endIndex)

            LoadResult.Page(
                data = pageImages,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (endIndex < images.size) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

