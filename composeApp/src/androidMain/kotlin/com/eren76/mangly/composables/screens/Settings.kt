package com.eren76.mangly.composables.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderModeType
import com.eren76.mangly.themes.setAppTheme

@Composable
fun Settings(
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ThemeSettings()

        HorizontalDivider(
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )

        ReadViewerSettings()

        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
        )

        LinkToHistoryManagementScreen(navController)

    }
}

@Composable
fun ThemeSettings() {
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

@Composable
fun ReadViewerSettings() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )
    }

    var selectedMode by remember {
        mutableStateOf(
            prefs.getString(
                ReaderModePrefs.KEY_READER_MODE,
                ReaderModePrefs.DEFAULT_READER_MODE_VALUE
            ) ?: ReaderModePrefs.DEFAULT_READER_MODE_VALUE
        )
    }

    Text(
        text = "Reader mode",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp)
    )

    val readerModeOptions = ReaderModeType.entries

    SingleChoiceSegmentedButtonRow {
        readerModeOptions.forEachIndexed { index, modeType ->
            SegmentedButton(
                selected = selectedMode == modeType.prefValue,
                onClick = {
                    selectedMode = modeType.prefValue
                    prefs.edit { putString(ReaderModePrefs.KEY_READER_MODE, modeType.prefValue) }
                },
                shape = SegmentedButtonDefaults.itemShape(index, readerModeOptions.size)
            ) {
                Text(modeType.displayName)
            }
        }
    }

    if (selectedMode == ReaderModeType.WEBTOON.prefValue) {
        ReadImagePreloadSetting(sharedPreferences = prefs)
    }
}

@Composable
fun ReadImagePreloadSetting(sharedPreferences: SharedPreferences) {
    val minAmount = 0
    val maxAmount = 7
    val settingKey = ReaderModePrefs.IMAGE_PRELOAD_AMOUNT

    var preloadAmount by rememberSaveable {
        mutableStateOf(sharedPreferences.getInt(settingKey, 2))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Image preload",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Controls how many images are loaded ahead while reading. " +
                    "Higher values use more memory but reduce loading delays.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Preload: $preloadAmount image${if (preloadAmount == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = preloadAmount.toFloat(),
            onValueChange = { preloadAmount = it.toInt() },
            onValueChangeFinished = {
                sharedPreferences.edit {
                    putInt(settingKey, preloadAmount)
                }
            },
            valueRange = minAmount.toFloat()..maxAmount.toFloat(),
            steps = maxAmount - minAmount - 1
        )
    }
}

@Composable
fun LinkToHistoryManagementScreen(
    navHostController: NavHostController
) {
    ElevatedCard(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Manage History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View your reading history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            FilledTonalButton(
                onClick = { navHostController.navigate("history") },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Open")
            }
        }
    }
}