package com.eren76.mangly.composables.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.SearchViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SearchQueryResult(
    val results: HashMap<ExtensionMetadata, List<Source.SearchResult>>,
    val errors: HashMap<ExtensionMetadata, String>
)

suspend fun querySearchFromSource(
    query: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
): SearchQueryResult {

    val results = HashMap<ExtensionMetadata, List<Source.SearchResult>>()
    val errors = HashMap<ExtensionMetadata, String>()

    withContext(Dispatchers.IO) {
        for (metadata in extensionMetadataViewModel.getAllSources()) {
            try {
                val searchResult = metadata.source.search(query)
                results[metadata] = searchResult
            } catch (e: Exception) {
                results[metadata] = emptyList()
                errors[metadata] = formatExtensionError(e)
            }
        }
    }

    return SearchQueryResult(results = results, errors = errors)
}

@Composable
fun Search(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    searchViewModel: SearchViewModel
) {
    val scope = rememberCoroutineScope()

    var textFieldState = remember { TextFieldState(searchViewModel.searchQuery) }
    var searchResults by searchViewModel::searchResults
    var searchErrors by searchViewModel::searchErrors
    var isLoading by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SimpleSearchBar(
            textFieldState = textFieldState,
            onSearch = { query ->
                scope.launch {
                    isLoading = true
                    try {
                        val searchResult = querySearchFromSource(query, extensionMetadataViewModel)
                        searchResults = searchResult.results
                        searchErrors = searchResult.errors
                        searchViewModel.updateSearchViewModel(
                            searchResult.results,
                            searchResult.errors,
                            query
                        )
                    } finally {
                        isLoading = false
                    }
                }
            },
            searchResults = searchResults,
            searchErrors = searchErrors,
            isLoading = isLoading,
            navHostController = navHostController,
            extensionMetadataViewModel = extensionMetadataViewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: HashMap<ExtensionMetadata, List<Source.SearchResult>>,
    searchErrors: HashMap<ExtensionMetadata, String>,
    isLoading: Boolean,
    navHostController: NavHostController,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchTriggered by remember { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        searchTriggered = false
                    },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        searchTriggered = true
                        expanded = true
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            if (isLoading || searchTriggered || searchResults.isNotEmpty()) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    if (isLoading) {
                        SearchResultsSkeleton()
                    } else {
                        searchResults.forEach { (extensionMetadata: ExtensionMetadata, results: List<Source.SearchResult>) ->
                            Text(
                                text = extensionMetadata.source.getExtensionName(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            val error = searchErrors[extensionMetadata]
                            if (error != null) {
                                Text(
                                    text = "Extension error: $error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            } else if (results.isEmpty()) {
                                Text(
                                    text = "No results found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            } else {
                                val listState = rememberLazyListState()

                                LaunchedEffect(searchTriggered) {
                                    if (searchTriggered) {
                                        listState.scrollToItem(0)
                                    }
                                }

                                LazyRow(
                                    state = listState,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(results) { result ->
                                        SearchResultCard(
                                            searchResult = result,
                                            onClick = {
                                                onItemClick(
                                                    result.url,
                                                    navHostController,
                                                    extensionMetadataViewModel,
                                                    extensionMetadata
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatExtensionError(error: Throwable): String {
    val message = error.message
        ?.takeIf { it.isNotBlank() }
        ?: error::class.java.simpleName

    return "${error::class.java.simpleName}: $message"
}

@Composable
fun SearchResultCard(
    searchResult: Source.SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            SearchResultImage(
                searchResult = searchResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = searchResult.title, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun SearchResultImage(
    searchResult: Source.SearchResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val headers: List<Source.Header> = searchResult.headers
    val networkHeaders = NetworkHeaders.Builder().apply {
        for (header in headers) {
            this[header.name] = header.value
        }
    }.build()

    val imageRequest = ImageRequest.Builder(context)
        .data(searchResult.imageUrl)
        .httpHeaders(networkHeaders)
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = searchResult.title,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            ImageLoadingComposable()
        }
    )
}


fun onItemClick(
    url: String,
    navController: NavHostController,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    correspondingSource: ExtensionMetadata
) {
    extensionMetadataViewModel.setSelectedSource(source = correspondingSource)

    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    navController.navigate("chapters/${encodedUrl}")
}
