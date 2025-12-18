package org.example.project.composables.screens.readviewer.webtoon

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.example.manglyextension.plugins.Source
import org.example.project.R
import org.example.project.viewmodels.ChaptersListViewModel

class WebtoonRecyclerAdapter(
    private val images: List<String>,
    private val headers: List<Source.Header>,
    private val imageLoader: ImageLoader,
    private val onPreviousChapter: () -> Unit = {},
    private val onNextChapter: () -> Unit = {},
    private val chaptersListViewModel: ChaptersListViewModel
) : RecyclerView.Adapter<WebtoonRecyclerAdapter.ComposeViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PREVIOUS = 0
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_NEXT = 2
    }

    class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_PREVIOUS
            itemCount - 1 -> VIEW_TYPE_NEXT
            else -> VIEW_TYPE_IMAGE
        }
    }

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

    override fun getItemCount(): Int = images.size + 2 // +2 for navigation boxes

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.composeView.translationZ = 10f
        holder.composeView.elevation = 10f
        holder.composeView.bringToFront()

        holder.composeView.setContent {
            when (getItemViewType(position)) {
                VIEW_TYPE_PREVIOUS -> NavigationBox(
                    text = "Previous Chapter",
                    onClick = onPreviousChapter,
                    chaptersListViewModel
                )

                VIEW_TYPE_NEXT -> NavigationBox(
                    text = "Next Chapter",
                    onClick = onNextChapter,
                    chaptersListViewModel
                )

                else -> {
                    val imageIndex = position - 1 // Adjust for the first navigation box
                    val imageUrl = images[imageIndex]
                    WebtoonItem(imageUrl, headers, imageLoader, "Chapter image ${imageIndex + 1}")
                }
            }
        }
    }
}

@Composable
private fun NavigationBox(
    text: String,
    onClick: () -> Unit,
    chaptersListViewModel: ChaptersListViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (text == "Previous Chapter") {
            Text(
                text = "Current chapter: " + chaptersListViewModel.getSelectedChapterNumber(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
        error = painterResource(R.drawable.outline_error_24)
    )
}
