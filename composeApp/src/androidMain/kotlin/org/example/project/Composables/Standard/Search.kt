package org.example.project.Composables.Standard

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.ViewModels.ExtensionMetadataViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

suspend fun querySearchFromSource(
    query: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
): HashMap<Source, List<Source.SearchResult>> {

    val results = HashMap<Source, List<Source.SearchResult>>()

    withContext(Dispatchers.IO) {
        for (metadata in extensionMetadataViewModel.getAllSources()) {
            val searchResult = metadata.source.search(query)
            results[metadata.source] = searchResult
        }
    }

    return results
}

@Composable
fun Search(extensionMetadataViewModel: ExtensionMetadataViewModel, navHostController: NavHostController) {
    val textFieldState = remember { TextFieldState() }
    var searchResults by remember { mutableStateOf(HashMap<Source, List<Source.SearchResult>>()) }
    val scope = CoroutineScope(Dispatchers.IO)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SimpleSearchBar(
            textFieldState = textFieldState,
            onSearch = { query ->
                scope.launch {
                    val results = querySearchFromSource(query, extensionMetadataViewModel)
                    searchResults = results
                }
            },
            searchResults = searchResults,
            navHostController = navHostController,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: HashMap<Source, List<Source.SearchResult>>,
    navHostController: NavHostController,
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
            if (searchTriggered) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    searchResults.forEach { (source, results) ->
                        Text(
                            text = source.getExtensionName(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { result ->
                                SearchResultCard(
                                    title = result.title,
                                    imageUrl = result.imageUrl,
                                    referer = source.getReferer(),
                                    onClick = {
                                        onItemClick(result.url, navHostController)
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

@Composable
fun SearchResultCard(
    title: String,
    imageUrl: String,
    referer: String,
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
                imageUrl = imageUrl,
                referer = referer,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun SearchResultImage(
    imageUrl: String,
    contentDescription: String,
    referer: String?,

    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // TODO: add user agent through source
    val headers = if (referer != null) {
        NetworkHeaders.Builder()
            .set("Referer", referer)
            .build()
    }
    else {
        NetworkHeaders.Builder()
            .build()
    }

    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .httpHeaders(headers)
        .crossfade(true)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}


fun onItemClick(url: String, navController: NavHostController) {
    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    navController.navigate("chapters/${encodedUrl}")
}