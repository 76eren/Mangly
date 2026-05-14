package com.eren76.mangly.composables.screens.settings

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.eren76.mangly.Constants
import com.eren76.mangly.themes.setAppTheme

@Composable
internal fun AppearanceSettingsSection() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            Constants.THEME_SETTING_KEY,
            Context.MODE_PRIVATE
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
