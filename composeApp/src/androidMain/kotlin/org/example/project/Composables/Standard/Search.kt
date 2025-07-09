package org.example.project.Composables.Standard

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
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
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Entities.ExtensionEntity
import java.io.File

suspend fun querySearchFromSource(
    query: String,
    context: Context
): HashMap<Source, List<Source.SearchResult>> {
    val extensionManager = ExtensionManager()
    val fileManager = FileManager()
    val results = HashMap<Source, List<Source.SearchResult>>()

    withContext(Dispatchers.IO) {
        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context)
        for (entry in allEntries) {
            val metadata: ExtensionMetadata = extensionManager.extractExtensionMetadata(File(entry.filePath), context)
            val searchResult = metadata.source.search(query)
            results[metadata.source] = searchResult
        }
    }

    return results
}

@Composable
fun Search() {
    val context = LocalContext.current
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
                    val results = querySearchFromSource(query, context)
                    searchResults = results
                }
            },
            searchResults = searchResults,
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
                                        textFieldState.edit { replace(0, length, result.title) }
                                        expanded = false
                                        searchTriggered = false
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
