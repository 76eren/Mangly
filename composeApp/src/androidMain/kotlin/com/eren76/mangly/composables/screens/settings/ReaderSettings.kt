package com.eren76.mangly.composables.screens.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.readviewer.ReaderModePrefs
import com.eren76.mangly.composables.screens.readviewer.ReaderModeType

@Composable
internal fun ReaderSettingsSection() {
    ReadViewerSettings()

    SettingsDivider()

    SettingDisableImageSavingOnHold()
}

@Composable
private fun ReadViewerSettings() {
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
}

@Composable
private fun SettingDisableImageSavingOnHold() {
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )
    }

    var isDisabled by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(
                ReaderModePrefs.DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY,
                false
            )
        )
    }

    fun updateSetting(value: Boolean) {
        isDisabled = value
        sharedPreferences.edit {
            putBoolean(
                ReaderModePrefs.DISABLE_IMAGE_SAVING_ON_HOLD_SETTING_KEY,
                value
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { updateSetting(!isDisabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Disable image saving on hold",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "When enabled, long-pressing an image in the reader will not show the save option. This helps prevent accidental saves.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = isDisabled,
            onCheckedChange = { updateSetting(it) }
        )
    }
}
