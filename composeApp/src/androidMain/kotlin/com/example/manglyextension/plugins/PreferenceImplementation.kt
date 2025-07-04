package com.example.manglyextension.plugins

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceImplementation(
    sharedPreferences: SharedPreferences,
    uiSharedPreferences: SharedPreferences,
    context: Context
) : IPreferences(
    sharedPreferences,
    uiSharedPreferences,
    context) {

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return settings.getBoolean(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean, uiElement: PreferenceUi?) {
        settings
            .edit {
                putBoolean(key, value)
            }

        uiElement?.let { bindKeyToUiElement(key, uiElement) }
    }


    override fun getString(key: String, defaultValue: String): String {
        return settings.getString(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String, uiElement: PreferenceUi?) {
        settings
            .edit {
                putString(key, value)
            }

        uiElement?.let { bindKeyToUiElement(key, uiElement) }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return settings.getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int, uiElement: PreferenceUi?) {
        settings
            .edit {
                putInt(key, value)
            }

        uiElement?.let { bindKeyToUiElement(key, uiElement) }
    }


    override fun bindKeyToUiElement(key: String, element: PreferenceUi) {
        uiSettings
            .edit {
                putString(key, element.toString())
            }
    }
}