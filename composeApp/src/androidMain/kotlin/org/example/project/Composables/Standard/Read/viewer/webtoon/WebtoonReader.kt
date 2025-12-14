package org.example.project.Composables.Standard.Read.viewer.webtoon

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.manglyextension.plugins.Source

@Composable
fun WebtoonReader(
    images: List<String>,
    headers: List<Source.Header>,
    modifier: Modifier = Modifier,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {}
) {
    val imageLoader = rememberStrongImageLoader()

    AndroidView<RecyclerView>(
        modifier = modifier,
        factory = { context: Context ->
            // Preload all images into cache
            images.forEach { url ->
                val req = buildImageRequest(context, url, headers)
                imageLoader.enqueue(req)
            }

            RecyclerView(context).apply {

                // This is so it doesn't load in real time when scrolling
                // This can also causes performance issues, I am currently not sure what the best approach would be
                // TODO: Find a better solution
                layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
                        isItemPrefetchEnabled = false // Prevent lazy loading
                    }
                setHasFixedSize(true)
                setItemViewCacheSize(images.size)
                setItemViewCacheSize(images.size)

                adapter = WebtoonRecyclerAdapter(
                    images,
                    headers,
                    imageLoader,
                    onPreviousChapter,
                    onNextChapter
                )
                recycledViewPool.setMaxRecycledViews(0, 0)


                translationZ = 20f
                elevation = 20f
                bringToFront()
            }
        },
        update = { recyclerView: RecyclerView ->
            recyclerView.adapter = WebtoonRecyclerAdapter(
                images,
                headers,
                imageLoader,
                onPreviousChapter,
                onNextChapter
            )

            // Preload new images
            images.forEach { url ->
                val req = buildImageRequest(recyclerView.context, url, headers)
                imageLoader.enqueue(req)
            }

            recyclerView.setItemViewCacheSize(images.size)
            recyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
            (recyclerView.layoutManager as? LinearLayoutManager)?.isItemPrefetchEnabled = false

            recyclerView.scrollToPosition(0)
        }
    )
}
