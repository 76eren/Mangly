package com.eren76.mangly.composables.screens.chapterslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChaptersSelectionBar(
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onSelectAll: () -> Unit,
    onApplySelection: () -> Unit,
    onDownloadOrDeleteSelection: () -> Unit,
    showDownloadUi: Boolean = true,
    isDownloadMode: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Select all chapters"
                    )
                    Text(text = "Select all")
                }
                TextButton(onClick = onApplySelection) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply chapter selection"
                    )
                    Text(text = "Mark")
                }
                if (showDownloadUi) {
                    TextButton(onClick = onDownloadOrDeleteSelection) {
                        Icon(
                            imageVector = (if (!isDownloadMode) Icons.Default.Add else Icons.Default.Delete),
                            contentDescription = if (!isDownloadMode) {
                                "Apply download selection"
                            } else {
                                "Apply delete selection"
                            }
                        )
                        Text(if (!isDownloadMode) "Download" else "Delete")
                    }
                }
            }
        }
    }
}

