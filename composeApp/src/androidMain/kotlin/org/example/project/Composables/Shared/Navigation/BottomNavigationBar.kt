package org.example.project.Composables.Shared.Navigation

import androidx.activity.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.example.project.Navigation.NavigationConstants

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    NavigationBar(

        containerColor = Color(0xFF0F9D58)
    ) {

        val navBackStackEntry by navController.currentBackStackEntryAsState()


        val currentRoute = navBackStackEntry?.destination?.route

        NavigationConstants.BottomNavItems.forEach { navItem ->

            NavigationBarItem(

                selected = currentRoute == navItem.route,

                onClick = {
                    navController.navigate(navItem.route)
                },

                icon = {
                    Icon(imageVector = navItem.icon, contentDescription = navItem.title)
                },

                label = {
                    Text(text = navItem.title)
                },
                alwaysShowLabel = false,

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White, // Icon color when selected
                    unselectedIconColor = Color.White, // Icon color when not selected
                    selectedTextColor = Color.White, // Label color when selected
                    indicatorColor = Color(0xFF195334) // Highlight color for selected item
                )
            )
        }
    }
}