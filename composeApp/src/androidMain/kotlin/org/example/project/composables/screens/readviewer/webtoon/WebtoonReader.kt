package org.example.project.composables.screens.readviewer.webtoon

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.manglyextension.plugins.Source
import org.example.project.viewmodels.ChaptersListViewModel

@Composable
fun WebtoonReader(
    images: List<String>,
    headers: List<Source.Header>,
    modifier: Modifier = Modifier,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    chaptersListViewModel: ChaptersListViewModel
) {
    val imageLoader = rememberStrongImageLoader()

    AndroidView<RecyclerView>(
        modifier = modifier,
        factory = { context: Context ->
            images.forEach { url ->
                val req = buildImageRequest(context, url, headers)
                imageLoader.enqueue(req)
            }

            RecyclerView(context).apply {
                layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
                        isItemPrefetchEnabled = false
                    }
                setHasFixedSize(true)
                setItemViewCacheSize(images.size)

                adapter = WebtoonRecyclerAdapter(
                    images,
                    headers,
                    imageLoader,
                    onPreviousChapter,
                    onNextChapter,
                    chaptersListViewModel
                )
                recycledViewPool.setMaxRecycledViews(0, 0)

                translationZ = 20f
                elevation = 20f
                bringToFront()
            }
        },
        update = { recyclerView: RecyclerView ->
            // Clear memory cache to release old images
            imageLoader.memoryCache?.clear()

            // Swap adapter with null first to force cleanup
            recyclerView.swapAdapter(null, true)

            recyclerView.adapter = WebtoonRecyclerAdapter(
                images,
                headers,
                imageLoader,
                onPreviousChapter,
                onNextChapter,
                chaptersListViewModel
            )

            images.forEach { url ->
                val req = buildImageRequest(recyclerView.context, url, headers)
                imageLoader.enqueue(req)
            }

            recyclerView.setItemViewCacheSize(images.size)
            recyclerView.recycledViewPool.clear()
            recyclerView.scrollToPosition(0)

            recyclerView.requestLayout()
        }
    )
}
