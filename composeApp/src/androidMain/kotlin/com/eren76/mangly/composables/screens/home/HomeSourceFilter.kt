package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.mangly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSourceFilter(
    sourceOptions: List<HomeSourceOption>,
    selectedSourceId: String?,
    onSelectedSourceChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val duplicateLabels: Set<String> = remember(sourceOptions) {
        sourceOptions
            .groupingBy { option -> option.displayName }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }
    val selectedLabel = sourceOptions
        .firstOrNull { option -> option.id == selectedSourceId }
        ?.displayName
        ?: "All sources"

    FilledTonalIconButton(
        onClick = { showSheet = true },
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.filter_list_24),
            contentDescription = "Filter by source. Current source: $selectedLabel"
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Filter by source",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Choose what appears in your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "all_sources") {
                    SourceFilterItem(
                        label = "All sources",
                        count = sourceOptions.sumOf { option -> option.itemCount },
                        selected = selectedSourceId == null,
                        onClick = {
                            showSheet = false
                            onSelectedSourceChange(null)
                        }
                    )
                }

                items(
                    items = sourceOptions,
                    key = { option -> option.id }
                ) { option ->
                    SourceFilterItem(
                        label = option.displayName,
                        count = option.itemCount,
                        selected = option.id == selectedSourceId,
                        supportingText = if (duplicateLabels.contains(option.displayName)) {
                            "Source ID ${option.id.take(8)}"
                        } else {
                            null
                        },
                        onClick = {
                            showSheet = false
                            onSelectedSourceChange(option.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceFilterItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        supportingContent = supportingText?.let { text ->
            {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null
            )
        },
        trailingContent = {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    )
}
