package com.eren76.mangly.composables.screens.history

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.Constants
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.util.SortedMap
import java.util.UUID

@Composable
fun HistoryManagement(
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    extensionMetaDataViewModel: ExtensionMetadataViewModel
) {
    val context = LocalContext.current

    val paginationPreferences = remember {
        context.getSharedPreferences(
            Constants.PAGINATION_SETTINGS_KEY,
            Context.MODE_PRIVATE
        )
    }

    val historyWithChapters by historyViewModel.historyWithChapters

    if (historyWithChapters.all { it.readChapters.isEmpty() }) {
        HistoryEmptyState()
        return
    }

    val historyData: SortedMap<Long, HistoryEntity> = remember(historyWithChapters) {
        getHistoryDataNewestToOldest(historyViewModel)
    }

    val sourcesById: Map<UUID, ExtensionMetadata> =
        remember(extensionMetaDataViewModel.getAllSources()) {
            extensionMetaDataViewModel.getAllSources().associateBy { metadata ->
                UUID.fromString(metadata.source.getExtensionId())
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider()


        if (!paginationPreferences.getBoolean(Constants.PAGINATION_ENABLED_KEY, false)) {
            ShowItemsInLazyGrid(
                historyData = historyData,
                sourcesById = sourcesById,
                historyViewModel = historyViewModel,
                navHostController = navHostController,
                extensionMetaDataViewModel = extensionMetaDataViewModel
            )
        } else {
            PaginatedHistoryList(
                historyData = historyData,
                sourcesById = sourcesById,
                historyViewModel = historyViewModel,
                navHostController = navHostController,
                extensionMetaDataViewModel = extensionMetaDataViewModel
            )
        }

    }
}

@Composable
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start reading a manga to see it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
