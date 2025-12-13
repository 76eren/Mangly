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
    modifier: Modifier = Modifier
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
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = WebtoonRecyclerAdapter(images, headers, imageLoader, prefetchCount = 5)
                setHasFixedSize(false)
                setItemViewCacheSize(images.size)
                recycledViewPool.setMaxRecycledViews(0, images.size)
                translationZ = 20f
                elevation = 20f
                bringToFront()
            }
        },
        update = { recyclerView: RecyclerView ->
            recyclerView.adapter =
                WebtoonRecyclerAdapter(images, headers, imageLoader, prefetchCount = 5)
            recyclerView.setItemViewCacheSize(images.size)
            recyclerView.recycledViewPool.setMaxRecycledViews(0, images.size)
            recyclerView.translationZ = 20f
            recyclerView.elevation = 20f
            recyclerView.bringToFront()
        }
    )
}
