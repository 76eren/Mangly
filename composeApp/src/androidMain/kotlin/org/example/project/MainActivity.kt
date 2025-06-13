package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import org.example.project.Composables.Shared.Navigation.BottomNavigationBar
import org.example.project.Composables.Shared.Navigation.NavHostContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            setContent {
                MaterialTheme {
                    val navController = rememberNavController()
                    Surface(color = Color.White) {
                        Scaffold(
                            bottomBar = {
                                BottomNavigationBar(navController = navController)
                            }, content = { padding ->
                                NavHostContainer(navController = navController, padding = padding)
                            }
                        )
                    }
                }
            }
        }
    }
}
