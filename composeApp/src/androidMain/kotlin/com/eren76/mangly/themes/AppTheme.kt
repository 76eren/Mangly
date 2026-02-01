package com.eren76.mangly.themes

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

private const val PREFS_NAME = "mangly_settings"
private const val THEME_KEY = "settings_theme"

private fun getPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun readTheme(context: Context): String =
    getPrefs(context).getString(THEME_KEY, "dark") ?: "dark"

private object ThemeStateHolder {
    var themeName by mutableStateOf<String>("dark")
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ThemeStateHolder.themeName = readTheme(context)
    }

    val scheme: ColorScheme = when (ThemeStateHolder.themeName) {
        "light" -> LightColorScheme
        "sakura" -> SakuraColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

fun setAppTheme(context: Context, theme: String) {
    getPrefs(context).edit { putString(THEME_KEY, theme) }
    ThemeStateHolder.themeName = theme
}
