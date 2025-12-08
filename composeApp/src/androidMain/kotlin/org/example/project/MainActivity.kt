package org.example.project

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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.manglyextension.plugins.ExtensionMetadata
import org.example.project.Composables.Shared.Navigation.BottomNavigationBar
import org.example.project.Composables.Shared.Navigation.NavHostContainer
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.Navigation.NavigationConstants
import org.example.project.Rooms.Entities.ExtensionEntity
import org.example.project.ViewModels.ExtensionDetailsViewModel
import org.example.project.ViewModels.ExtensionMetadataViewModel
import org.example.project.ViewModels.SearchViewModel
import org.example.project.Themes.AppTheme
import java.io.File

class MainActivity : ComponentActivity() {
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
                    val routesThatShouldShowBottomBar: List<String> = NavigationConstants.BottomNavItems.map { it.route }
                    val showBottomBar: Boolean = currentRoute in routesThatShouldShowBottomBar


                    // Define viewModels
                    val extensionDetailsViewModel: ExtensionDetailsViewModel = viewModel()
                    val sourcesViewModel: ExtensionMetadataViewModel = viewModel()
                    val searchViewModel: SearchViewModel = viewModel()


                    // Populate data for view models if needed
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
                                extensionDetailsViewModel,
                                sourcesViewModel,
                                searchViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}


suspend fun fetchSources(context: Context): List<ExtensionMetadata> {
    val fileManager = FileManager()
    val extensionManager = ExtensionManager()
    val sources = mutableListOf<ExtensionMetadata>()

    val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context)
    for (entry in allEntries) {
        val metadata: ExtensionMetadata = extensionManager.extractExtensionMetadata(File(entry.filePath), context)
        sources.add(metadata)
    }

    return sources
}