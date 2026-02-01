package org.example.project.composables.screens

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
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.viewmodels.ExtensionMetadataViewModel
import org.example.project.viewmodels.SearchViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

suspend fun querySearchFromSource(
    query: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
): HashMap<ExtensionMetadata, List<Source.SearchResult>> {

    val results = HashMap<ExtensionMetadata, List<Source.SearchResult>>()

    withContext(Dispatchers.IO) {
        for (metadata in extensionMetadataViewModel.getAllSources()) {
            try {
                val searchResult = metadata.source.search(query)
                results[metadata] = searchResult
            } catch (e: Exception) {
                // This can trigger if the source itself does not have proper error handling
                results[metadata] = emptyList()
            }
        }
    }

    return results
}

@Composable
fun Search(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    searchViewModel: SearchViewModel
) {
    val scope = CoroutineScope(Dispatchers.IO)

    var textFieldState = remember { TextFieldState(searchViewModel.searchQuery) }
    var searchResults by searchViewModel::searchResults


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
                    val results = querySearchFromSource(query, extensionMetadataViewModel)
                    searchResults = results
                    searchViewModel.updateSearchViewModel(results, query)
                }
            },
            searchResults = searchResults,
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
            if (searchTriggered || searchResults.isNotEmpty()) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    searchResults.forEach { (extensionMetadata: ExtensionMetadata, results: List<Source.SearchResult>) ->
                        Text(
                            text = extensionMetadata.source.getExtensionName(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (results.isEmpty()) {
                            Text(
                                text = "No results found or an error occurred.",
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

    AsyncImage(
        model = imageRequest,
        contentDescription = searchResult.title,
        modifier = modifier,
        contentScale = ContentScale.Crop
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