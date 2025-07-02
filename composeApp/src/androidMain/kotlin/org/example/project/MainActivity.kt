package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import org.example.project.Composables.Shared.Navigation.BottomNavigationBar
import org.example.project.Composables.Shared.Navigation.NavHostContainer
import org.example.project.ViewModels.ExtensionDetailsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            setContent {
                MaterialTheme {
                    val navController = rememberNavController()

                    // Define viewModels
                    val extensionDetailsViewModel: ExtensionDetailsViewModel = viewModel()

                    Surface(color = Color.White) {
                        Scaffold(
                            bottomBar = {
                                BottomNavigationBar(navController = navController)
                            }, content = { padding ->
                                NavHostContainer(navController = navController, padding = padding, extensionDetailsViewModel)
                            }
                        )
                    }
                }
            }
        }
    }
}
