package org.example.project.Composables.Standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.example.project.Composables.Shared.Topbar.TopBarNewExtension

@Composable
fun Extensions() {
    Scaffold (
        topBar = { TopBarNewExtension() }
    ){ paddingValues ->

        Column ( modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.White),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Extensions page",
                tint = Color(0xFF0F9D58)
            )

        }
    }


}