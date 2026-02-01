package com.eren76.mangly

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eren76.mangly.composables.shared.navigation.BottomNavigationBar
import com.eren76.mangly.navigation.NavHostContainer
import com.eren76.mangly.navigation.NavigationConstants
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.mangly.themes.AppTheme
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.ExtensionDetailsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.mangly.viewmodels.SearchViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fileManager: FileManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MaterialTheme {
                    // Picks when the bottom bar should be shown
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val currentRoute = navBackStackEntry?.destination?.route
                    val routesThatShouldShowBottomBar: List<String> =
                        NavigationConstants.BottomNavItems.map { it.route }
                    val showBottomBar: Boolean = currentRoute in routesThatShouldShowBottomBar

                    // Define viewModels via Hilt
                    val extensionDetailsViewModel: ExtensionDetailsViewModel = hiltViewModel()
                    val sourcesViewModel: ExtensionMetadataViewModel = hiltViewModel()
                    val searchViewModel: SearchViewModel = hiltViewModel()
                    val chaptersListViewModel: ChaptersListViewModel = hiltViewModel()
                    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
                    val historyViewModel: HistoryViewModel = hiltViewModel()


                    // Populate data for view models
                    LaunchedEffect(Unit) {
                        val metadataList = fetchSources(applicationContext)
                        sourcesViewModel.setSources(metadataList)
                    }

                    Surface(color = MaterialTheme.colorScheme.background) {
                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    BottomNavigationBar(navController = navController)
                                }
                            }
                        ) { padding ->
                            NavHostContainer(
                                navController = navController,
                                padding = padding,
                                extensionsViewModel = extensionDetailsViewModel,
                                extensionMetadataViewModel = sourcesViewModel,
                                searchViewModel = searchViewModel,
                                chaptersListViewModel = chaptersListViewModel,
                                favoritesViewModel = favoritesViewModel,
                                historyViewModel = historyViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun fetchSources(context: Context): List<ExtensionMetadata> {
        val sources = mutableListOf<ExtensionMetadata>()

        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context)
        for (entry in allEntries) {
            val metadata: ExtensionMetadata =
                extensionManager.extractExtensionMetadata(File(entry.filePath).readBytes(), context)
            sources.add(metadata)
        }

        return sources
    }
}