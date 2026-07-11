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

@Composable
fun HomeSourceFilter(
    sourceOptions: List<HomeSourceOption>,
    selectedSourceId: String?,
    onSelectedSourceChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val duplicateLabels: Set<String> = remember(sourceOptions) {
        sourceOptions
            .groupingBy { option -> option.displayName }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }

    val selectedLabel: String = sourceOptions
        .firstOrNull { option -> option.id == selectedSourceId }
        ?.let { option -> labelForSource(option, duplicateLabels) }
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
                text = { Text("All sources (${sourceOptions.sumOf { it.itemCount }})") },
                onClick = {
                    expanded = false
                    onSelectedSourceChange(null)
                }
            )

            sourceOptions.forEach { option ->
                val label: String = labelForSource(option, duplicateLabels)

                DropdownMenuItem(
                    text = { Text("$label (${option.itemCount})") },
                    onClick = {
                        expanded = false
                        onSelectedSourceChange(option.id)
                    }
                )
            }
        }
    }
}

private fun labelForSource(
    option: HomeSourceOption,
    duplicateLabels: Set<String>
): String {
    val baseLabel = option.displayName

    return if (duplicateLabels.contains(baseLabel)) {
        "$baseLabel (${option.id.take(8)})"
    } else {
        baseLabel
    }
}
