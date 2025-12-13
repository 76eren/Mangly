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
    AndroidView<RecyclerView>(
        modifier = modifier,
        factory = { context: Context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = WebtoonRecyclerAdapter(images, headers)
                setHasFixedSize(false)
                translationZ = 20f
                elevation = 20f
                bringToFront()
            }
        },
        update = { recyclerView: RecyclerView ->
            recyclerView.adapter = WebtoonRecyclerAdapter(images, headers)
            recyclerView.translationZ = 20f
            recyclerView.elevation = 20f
            recyclerView.bringToFront()
        }
    )
}
