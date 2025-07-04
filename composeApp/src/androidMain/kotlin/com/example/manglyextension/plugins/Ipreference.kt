package com.example.manglyextension.plugins

import android.content.Context
import android.content.SharedPreferences
import java.util.Objects

abstract class IPreferences(settingsPreferences: Any?, uiPreferences: Any?, ctx: Any?) {
    var settings: SharedPreferences = settingsPreferences as SharedPreferences
    var uiSettings: SharedPreferences = uiPreferences as SharedPreferences
    var context: Context = ctx as Context

    abstract fun getBoolean(key: String, defaultValue: Boolean): Boolean
    abstract fun setBoolean(key: String, value: Boolean, uiElement: PreferenceUi? = null)

    abstract fun getString(key: String, defaultValue: String): String
    abstract fun setString(key: String, value: String, uiElement: PreferenceUi? = null)

    abstract fun getInt(key: String, defaultValue: Int): Int
    abstract fun setInt(key: String, value: Int, uiElement: PreferenceUi? = null)

    abstract fun bindKeyToUiElement(key: String, element: PreferenceUi)
}

enum class PreferenceUi {
    TEXTAREA,
    SWITCH
}