package org.example.project.Composables.Standard.ChaptersList

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.ViewModels.ExtensionMetadataViewModel

@Composable
fun ChaptersList(targetUrl: String, extensionMetadataViewModel: ExtensionMetadataViewModel) {
    Log.d("test", "Selected source is: ${extensionMetadataViewModel.selectedSingleSource.value?.source?.getExtensionName()}")

//    val chapters: List<Source.ChapterValue> = remember { ArrayList() }
//    var imageUrl = remember { "" }
//
//    LaunchedEffect(Unit) {
//
//    }


}


fun fetchChapterImage(source: Source, url: String): ImageForChaptersList {
    return source.getImageForChaptersList(url)
}

fun chapterList(source: Source, url: String): List<Source.ChapterValue> {
    return source.getChaptersFromChapterUrl(url)
}