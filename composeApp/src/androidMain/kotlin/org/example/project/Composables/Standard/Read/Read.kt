package org.example.project.Composables.Standard.Read

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.ViewModels.ExtensionMetadataViewModel

@Composable
fun Read(targetUrl: String, extensionMetadataViewModel: ExtensionMetadataViewModel) {
    var chapterImagses by remember { mutableStateOf<Source.ChapterImages?>(null) }

    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value

    LaunchedEffect(targetUrl, metadata) {
        if (metadata != null) {
            chapterImagses = withContext(Dispatchers.IO) {
                getChapterImages(targetUrl, metadata)
            }
        } else {
            chapterImagses = Source.ChapterImages(emptyList(), emptyList())
        }

    }

    // For testing
    Text(chapterImagses.let {
        if (it != null) {
            it.images[0]
        } else {
            "Failed to load chapter images."
        }
    })

}


suspend fun getChapterImages(url: String, metadata: ExtensionMetadata): Source.ChapterImages {
    try {
        val data =  metadata.source.getChapterImages(url)
        return data
    }
    catch (e: Exception) {
        return Source.ChapterImages(emptyList(), emptyList())
    }
}
