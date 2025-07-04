package org.example.project.Composables.Standard.ExtensionDetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manglyextension.plugins.PreferenceImplementation
import com.example.manglyextension.plugins.PreferenceUi
import com.example.manglyextension.plugins.Source
import org.example.project.Composables.Shared.Topbar.TopBarDeleteExtensionFromExtensionDetails
import org.example.project.Composables.Standard.CardData


@Composable
fun ExtensionDetails(cardData: CardData) {
    val allUiSettings = remember { seedSettings(cardData) }
    val prefs = remember { cardData.source.preferences as PreferenceImplementation }

    Scaffold(
        topBar = { TopBarDeleteExtensionFromExtensionDetails(cardData.metadata) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cardData.title,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            for (setting in allUiSettings) {
                when (setting.uiElement) {
                    PreferenceUi.SWITCH   -> PreferenceSwitch(setting, prefs)
                    PreferenceUi.TEXTAREA -> PreferenceTextArea(setting, prefs)
                    else                  -> { }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    setting: Source.SettingGen,
    prefs: PreferenceImplementation,
    modifier: Modifier = Modifier
) {
    val key = setting.key
    var checked by remember(key) { mutableStateOf(prefs.getBoolean(key, false)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = setting.key,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                checked = newValue
                prefs.setBoolean(key, newValue)
            }
        )
    }
}

@Composable
private fun PreferenceTextArea(
    setting: Source.SettingGen,
    prefs: PreferenceImplementation,
    modifier: Modifier = Modifier
) {
    val key = setting.key
    var text by remember(key) { mutableStateOf(prefs.getString(key, "")) }
    var expanded by remember(key) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = setting.key,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Done" else "Edit")
            }
        }

        if (expanded) {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    prefs.setString(key, newText, uiElement = setting.uiElement)
                },
                label = { Text("Enter ${setting.key}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        } else if (text.isNotBlank()) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

fun seedSettings(cardData: CardData): List<Source.SettingGen> {
    val rawSettings = cardData.source.generateSettings()
    val preferences: PreferenceImplementation = cardData.source.preferences as PreferenceImplementation

    // If the UI settings aren't present yet, we initialize them
    for (setting in rawSettings) {
        if (!preferences.settings.contains(setting.key)) {
            when (val settingUnknownType = setting.defaultValue) {
                is Boolean -> preferences.setBoolean(key = setting.key, value = settingUnknownType, uiElement = setting.uiElement)
                is String -> preferences.setString(key = setting.key, value = settingUnknownType, uiElement = setting.uiElement)
                is Int -> preferences.setInt(key = setting.key, value = settingUnknownType, uiElement = setting.uiElement)
                else -> throw IllegalArgumentException("Unsupported type for setting: ${settingUnknownType::class.java}")
            }
        }
    }

    return rawSettings
}