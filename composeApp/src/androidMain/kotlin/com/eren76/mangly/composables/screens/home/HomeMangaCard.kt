package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.dropdowns.DeleteDropdownMenu
import com.eren76.manglyextension.plugins.ExtensionMetadata

@Composable
fun HomeMangaCard(
    title: String,
    menuKey: Any,
    menuText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    imageContent: @Composable (Modifier) -> Unit,
    extensionMetadata: ExtensionMetadata?,
    subtitle: String? = null,
    allowOpeningWithoutSource: Boolean = false
) {
    var isDeleteDropdownMenuExpanded by remember(menuKey) { mutableStateOf(false) }

    val canOpenCard = allowOpeningWithoutSource || extensionMetadata?.canSourceBeLoaded == true
    val isSourceDisabled = extensionMetadata?.canSourceBeLoaded == false

    Card(
        modifier = Modifier.width(160.dp).height(220.dp)
            .combinedClickable(
                onClickLabel = if (canOpenCard) null else "Show delete options",
                onClick = { if (canOpenCard) onClick() else isDeleteDropdownMenuExpanded = true },
                onLongClick = { isDeleteDropdownMenuExpanded = true }
            ),
        elevation = CardDefaults.cardElevation(4.dp),
        border = if (isSourceDisabled) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            imageContent(Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = if (subtitle == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSourceDisabled) {
                Text(
                    text = "Extension needs updating",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.align(Alignment.TopStart)
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DeleteDropdownMenu(
                    expanded = isDeleteDropdownMenuExpanded,
                    onDismissRequest = { isDeleteDropdownMenuExpanded = false },
                    text = menuText,
                    onDeleteClick = {
                        isDeleteDropdownMenuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
