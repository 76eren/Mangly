package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.manglyextension.plugins.ExtensionMetadata

@Composable
fun HomeSourceFilter(
    itemCountsBySource: Map<ExtensionMetadata, Int>,
    selectedSource: ExtensionMetadata?,
    onSelectedSourceChange: (ExtensionMetadata?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val sources = remember(itemCountsBySource) {
        itemCountsBySource.keys.sortedWith(
            compareBy<ExtensionMetadata>(
                { source -> source.displayName().lowercase() },
                { source -> source.source.getExtensionId() }
            )
        )
    }
    val baseLabelsBySource: Map<ExtensionMetadata, String> = remember(sources) {
        sources.associateWith { source -> source.displayName() }
    }
    val duplicateLabels: Set<String> = remember(baseLabelsBySource) {
        baseLabelsBySource.values
            .groupingBy { label -> label }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }

    val selectedLabel: String = selectedSource
        ?.let { source: ExtensionMetadata ->
            labelForSource(
                source = source,
                baseLabelsBySource = baseLabelsBySource,
                duplicateLabels = duplicateLabels
            )
        }
        ?: "All sources"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Source: $selectedLabel",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Open source filter"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All sources (${itemCountsBySource.values.sum()})") },
                onClick = {
                    expanded = false
                    onSelectedSourceChange(null)
                }
            )

            sources.forEach { source ->
                val label: String = labelForSource(
                    source = source,
                    baseLabelsBySource = baseLabelsBySource,
                    duplicateLabels = duplicateLabels
                )
                val itemCount = itemCountsBySource[source] ?: 0

                DropdownMenuItem(
                    text = { Text("$label ($itemCount)") },
                    onClick = {
                        expanded = false
                        onSelectedSourceChange(source)
                    }
                )
            }
        }
    }
}

private fun labelForSource(
    source: ExtensionMetadata,
    baseLabelsBySource: Map<ExtensionMetadata, String>,
    duplicateLabels: Set<String>
): String {
    val baseLabel = baseLabelsBySource[source] ?: source.displayName()

    return if (duplicateLabels.contains(baseLabel)) {
        "$baseLabel (${
            source.source.getExtensionId().take(8)
        })" // Append first 8 characters of extension ID to disambiguate
    } else {
        baseLabel
    }
}

private fun ExtensionMetadata.displayName(): String {
    val metadataName = name.takeIf { value -> value.isNotBlank() }
    if (metadataName != null) return metadataName

    return runCatching { source.getExtensionName() }
        .getOrNull()
        ?.takeIf { value -> value.isNotBlank() }
        ?: "Unknown source"
}
