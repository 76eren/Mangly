package com.eren76.mangly.search

import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal data class SearchQueryResult(
    val results: Map<ExtensionMetadata, List<Source.SearchResult>>,
    val errors: Map<ExtensionMetadata, String>
)

private data class ExtensionSearchResult(
    val metadata: ExtensionMetadata,
    val results: List<Source.SearchResult>,
    val error: String?
)

internal suspend fun querySearchSources(
    query: String,
    sources: List<ExtensionMetadata>
): SearchQueryResult = coroutineScope {
    val sourceResults: List<ExtensionSearchResult> = sources.map { metadata ->
        async(Dispatchers.IO) {
            try {
                ExtensionSearchResult(
                    metadata = metadata,
                    results = metadata.source.search(query),
                    error = null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ExtensionSearchResult(
                    metadata = metadata,
                    results = emptyList(),
                    error = formatExtensionError(error)
                )
            }
        }
    }.awaitAll()

    val results = mutableMapOf<ExtensionMetadata, List<Source.SearchResult>>()
    val errors = mutableMapOf<ExtensionMetadata, String>()

    for (sourceResult in sourceResults) {
        results[sourceResult.metadata] = sourceResult.results

        val error = sourceResult.error
        if (error != null) {
            errors[sourceResult.metadata] = error
        }
    }

    return@coroutineScope SearchQueryResult(
        results = results,
        errors = errors
    )
}

private fun formatExtensionError(error: Throwable): String {
    val message = error.message
        ?.takeIf { it.isNotBlank() }
        ?: error::class.java.simpleName

    return "${error::class.java.simpleName}: $message"
}
