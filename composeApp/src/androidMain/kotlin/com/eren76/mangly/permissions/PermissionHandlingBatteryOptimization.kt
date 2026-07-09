package com.eren76.mangly.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.edit

object BatteryOptimizationPermissionHandling {
    // if the dialog to enable battery optimization gets denied once we don't want to show it again, so we store that in shared preferences
    private const val PREFERENCES_NAME = "battery_optimization_permissions"
    private const val BACKGROUND_DOWNLOAD_PROMPT_DISMISSED =
        "background_download_prompt_dismissed"

    fun canRequestBatteryOptimizationExemption(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (!canRequestBatteryOptimizationExemption()) return true

        val powerManager = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false

        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun shouldShowBackgroundDownloadPrompt(context: Context): Boolean {
        return canRequestBatteryOptimizationExemption() &&
                !isIgnoringBatteryOptimizations(context) &&
                !isBackgroundDownloadPromptDismissed(context)
    }

    fun dismissBackgroundDownloadPrompt(context: Context) {
        promptPreferences(context).edit {
            putBoolean(BACKGROUND_DOWNLOAD_PROMPT_DISMISSED, true)
        }
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        if (!canRequestBatteryOptimizationExemption()) return false

        val packageUri = Uri.parse("package:${context.packageName}")
        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = packageUri
        }
        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        }
        val batteryOptimizationIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        return listOf(
            requestIntent,
            batteryOptimizationIntent,
            appSettingsIntent
        ).any { intent ->
            startSettingsIntent(context, intent)
        }
    }

    private fun isBackgroundDownloadPromptDismissed(context: Context): Boolean {
        return promptPreferences(context).getBoolean(
            BACKGROUND_DOWNLOAD_PROMPT_DISMISSED,
            false
        )
    }

    private fun promptPreferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun startSettingsIntent(
        context: Context,
        intent: Intent
    ): Boolean {
        val safeIntent = intent.apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return runCatching {
            context.startActivity(safeIntent)
            true
        }.getOrElse {
            false
        }
    }
}
