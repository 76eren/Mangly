package org.example.project.ViewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.manglyextension.plugins.ExtensionMetadata

class ExtensionMetadataViewModel : ViewModel() {
    private val sources = mutableStateListOf<ExtensionMetadata>()

    fun setSources(data: List<ExtensionMetadata>) {
        sources.clear()
        sources.addAll(data)
    }

    fun getAllSources(): List<ExtensionMetadata> {
        return sources
    }
}