package org.example.project.Composables.Shared.Topbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun TopBarNewExtension() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 25.dp)
            .height(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IconButton(
            onClick = {

            },
            modifier = Modifier
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
}