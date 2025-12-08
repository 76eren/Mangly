package org.example.project.Composables.Standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import org.example.project.Themes.setAppTheme

@Composable
fun Settings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ThemeSettings()
    }
}

@Composable
fun ThemeSettings() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "mangly_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var selectedTheme by remember {
        mutableStateOf(
            prefs.getString("settings_theme", "dark") ?: "dark"
        )
    }
    
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings page",
    )

    Text(
        text = "Theme",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    val options = listOf("dark", "light", "sakura")
    val labels = mapOf("dark" to "Dark", "light" to "Light", "sakura" to "Sakura")

    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, value ->
            val label = labels[value] ?: value.replaceFirstChar { it.uppercase() }

            SegmentedButton(
                selected = selectedTheme == value,
                onClick = {
                    selectedTheme = value
                    prefs.edit { putString("settings_theme", value) }
                    setAppTheme(context, value)
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }

    Text(
        text = "Pick a theme. Default is Dark.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}