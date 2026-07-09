package com.eren76.mangly.composables.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.screens.home.HomeSorting
import com.eren76.mangly.permissions.BatteryOptimizationPermissionHandling

@Composable
internal fun LibrarySettingsSection(
    navController: NavHostController,
    isDownloadModeEnabled: Boolean,
    onDownloadModeChanged: (Boolean) -> Unit,
) {
    HomePageSorting()

    SettingsDivider()

    LinkToHistoryManagementScreen(navController)

    SettingsDivider()

    EnablePagination()

    SettingsDivider()

    DownloadModeSetting(
        isEnabled = isDownloadModeEnabled,
        onEnabledChanged = onDownloadModeChanged
    )

    if (isDownloadModeEnabled) {
        LinkToDownloadsScreen(navController)
        
        BatteryOptimizationSetting()

        SettingsDivider()
    }
}

@Composable
private fun HomePageSorting() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(Constants.HOME_SETTING_KEY, Context.MODE_PRIVATE)
    }
    val defaultValue = HomeSorting.DEFAULT_PREF_VALUE
    var selectedSorting by remember {
        mutableStateOf(
            prefs.getString(Constants.HOME_SORTING_SETTING_KEY, defaultValue) ?: defaultValue
        )
    }
    val options = HomeSorting.entries
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = options.first { it.prefValue == selectedSorting }.label
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Home page sorting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                options.forEach { sorting ->
                    val isSelected = selectedSorting == sorting.prefValue
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "contentColor"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSorting = sorting.prefValue
                                prefs.edit {
                                    putString(
                                        Constants.HOME_SORTING_SETTING_KEY,
                                        sorting.prefValue
                                    )
                                }
                                expanded = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sorting.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkToHistoryManagementScreen(
    navHostController: NavHostController
) {
    SettingsNavigationCard(
        title = "Manage History",
        description = "View your reading history",
        buttonText = "Open",
        onClick = { navHostController.navigate("history") },
    )
}

@Composable
private fun LinkToDownloadsScreen(
    navHostController: NavHostController
) {
    SettingsNavigationCard(
        title = "Manage downloads",
        description = "View your downloads",
        buttonText = "Open",
        onClick = { navHostController.navigate("home/downloads") },
    )
}

@Composable
private fun SettingsNavigationCard(
    title: String,
    description: String,
    buttonText: String,
    buttonEnabled: Boolean = true,
    onClick: () -> Unit,
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            FilledTonalButton(
                onClick = onClick,
                enabled = buttonEnabled,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun BatteryOptimizationSetting() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            BatteryOptimizationPermissionHandling.isIgnoringBatteryOptimizations(context)
        )
    }

    fun refreshBatteryOptimizationState() {
        isIgnoringBatteryOptimizations =
            BatteryOptimizationPermissionHandling.isIgnoringBatteryOptimizations(context)
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshBatteryOptimizationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsNavigationCard(
        title = "Background downloads",
        description = if (isIgnoringBatteryOptimizations) {
            "Battery optimization is disabled for Mangly."
        } else {
            "Disable battery optimization for more reliable background downloads."
        },
        buttonText = if (isIgnoringBatteryOptimizations) "Enabled" else "Open",
        buttonEnabled = !isIgnoringBatteryOptimizations,
        onClick = {
            BatteryOptimizationPermissionHandling.openBatteryOptimizationSettings(context)
        },
    )
}

@Composable
private fun EnablePagination() {
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences(
            Constants.PAGINATION_SETTINGS_KEY,
            Context.MODE_PRIVATE
        )
    }

    var isDisabled by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(
                Constants.PAGINATION_ENABLED_KEY,
                false
            )
        )
    }

    fun updateSetting(value: Boolean) {
        isDisabled = value
        sharedPreferences.edit {
            putBoolean(
                Constants.PAGINATION_ENABLED_KEY,
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
                text = "Enable pagination",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "When enabled, all items on both the home page and history page get paginated. This helps if you are overwhelmed by the amount of items shown at once.",
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

@Composable
private fun DownloadModeSetting(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnabledChanged(!isEnabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Enable download mode (Beta)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Downloading is currently beta. Expect issues and missing features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onEnabledChanged(it) }
        )
    }
}
