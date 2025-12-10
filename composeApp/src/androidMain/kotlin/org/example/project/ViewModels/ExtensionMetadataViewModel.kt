package org.example.project.ViewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtensionMetadataViewModel @Inject constructor() : ViewModel() {
    private val sources = mutableStateListOf<ExtensionMetadata>()

    var selectedSingleSource = mutableStateOf<ExtensionMetadata?>(null)

    fun setSources(data: List<ExtensionMetadata>) {
        sources.clear()
        sources.addAll(data)
    }

    fun getAllSources(): List<ExtensionMetadata> {
        return sources
    }

    fun setSelectedSource(source: ExtensionMetadata) {
        selectedSingleSource.value = source
    }
}