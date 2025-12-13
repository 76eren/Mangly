package org.example.project.Composables.Standard.Read.viewer.webtoon

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.example.manglyextension.plugins.Source

class WebtoonRecyclerAdapter(
    private val images: List<String>,
    private val headers: List<Source.Header>,
    private val imageLoader: ImageLoader,
) : RecyclerView.Adapter<WebtoonRecyclerAdapter.ComposeViewHolder>() {

    class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
        val composeView = ComposeView(parent.context)
        composeView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        composeView.translationZ = 10f
        composeView.elevation = 10f
        composeView.bringToFront()
        return ComposeViewHolder(composeView)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        val imageUrl = images[position]

        holder.setIsRecyclable(false)
        holder.composeView.translationZ = 10f
        holder.composeView.elevation = 10f
        holder.composeView.bringToFront()

        holder.composeView.setContent {
            WebtoonItem(imageUrl, headers, imageLoader, "Chapter image ${position + 1}")
        }
    }
}

@Composable
private fun WebtoonItem(
    imageUrl: String,
    headers: List<Source.Header>,
    imageLoader: ImageLoader,
    contentDescription: String,
) {
    val request = buildImageRequest(LocalContext.current, imageUrl, headers)

    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp),
        contentScale = ContentScale.FillWidth,
    )
}
