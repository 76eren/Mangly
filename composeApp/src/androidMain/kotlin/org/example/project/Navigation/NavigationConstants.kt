package org.example.project.Navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon

object NavigationConstants {
    val BottomNavItems = listOf(

        NavigationItem(
            "Home",
            Icons.Filled.Home,
            route = "home"
        ),

        NavigationItem(
            "Sources",
            Icons.Default.Info,
            route = "sources"
        ),

        NavigationItem(
            "Search",
            Icons.Filled.Search,
            route = "search"
        ),

        NavigationItem(
            "Settings",
            Icons.Filled.Settings,
            route = "settings"
        )


    )
}