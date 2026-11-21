package com.eren76.mangly.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eren76.manglyextension.plugins.ExtensionLoadResult
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.LoadedSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtensionMetadataViewModel @Inject constructor() : ViewModel() {
    private var allSourceLoadResultsById by mutableStateOf<Map<String, ExtensionLoadResult>>(
        emptyMap()
    )
    private var selectedSourceId by mutableStateOf<String?>(null)

    val isLoading = mutableStateOf(true)

    val selectedWorkingSource: LoadedSource?
        get() = selectedSourceId?.let { getWorkingSourceById(it) }

    fun setAllSourceLoadResults(allSourceLoadResults: List<ExtensionLoadResult>) {
        allSourceLoadResultsById = allSourceLoadResults.associateBy { it.metadata.extensionId }
        isLoading.value = false
    }

    /** All source metadata, including disabled sources, for saved items and extension management. */
    fun getAllValidAndInvalidSourcesMetadata(): List<ExtensionMetadata> =
        allSourceLoadResultsById.values.map { it.metadata }

    /** Working sources have passed compatibility validation and were instantiated successfully. */
    fun getOnlyValidSourcesMetadata(): List<LoadedSource> =
        allSourceLoadResultsById.values.mapNotNull { it.loadedSource }

    /** Metadata remains accessible even when the extension is disabled. */
    fun getSourceMetadataById(extensionId: String): ExtensionMetadata? =
        allSourceLoadResultsById[extensionId]?.metadata

    /** Returns null when the extension is disabled or its ID is absent from the registry. */
    fun getWorkingSourceById(extensionId: String): LoadedSource? =
        allSourceLoadResultsById[extensionId]?.loadedSource

    fun selectSource(extensionId: String) {
        selectedSourceId = extensionId
    }
}
