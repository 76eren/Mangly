package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtensionMetadataViewModel @Inject constructor() : ViewModel() {
    private val sources = mutableStateOf<List<ExtensionMetadata>>(emptyList())
    private var sourcesById: Map<String, ExtensionMetadata> = emptyMap()

    val isLoading = mutableStateOf(true)
    var selectedSingleSource = mutableStateOf<ExtensionMetadata?>(null)

    fun setSources(data: List<ExtensionMetadata>) {
        val sourceSnapshot = data.toList()
        sourcesById = buildMap {
            for (metadata in sourceSnapshot) {
                val sourceId = runCatching { metadata.source.getExtensionId() }
                    .getOrNull()
                    ?.takeIf { id -> id.isNotBlank() }
                    ?: continue
                put(sourceId, metadata)
            }
        }
        sources.value = sourceSnapshot
        isLoading.value = false
    }

    fun getAllSources(): List<ExtensionMetadata> {
        return sources.value
    }

    fun getSourceById(extensionId: String): ExtensionMetadata? {
        return sourcesById[extensionId]
    }

    fun setSelectedSource(source: ExtensionMetadata) {
        selectedSingleSource.value = source
    }
}
