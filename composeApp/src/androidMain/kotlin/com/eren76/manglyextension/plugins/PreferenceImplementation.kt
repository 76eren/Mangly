package com.eren76.manglyextension.plugins

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceImplementation(
    sharedPreferences: SharedPreferences,
    context: Context
) : IPreferences(
    sharedPreferences,
    context
) {
    private val sharedPreferences: SharedPreferences
        get() = settings as SharedPreferences

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean, uiElement: PreferenceUi?) {
        sharedPreferences
            .edit {
                putBoolean(key, value)
            }

    }

    override fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String, uiElement: PreferenceUi?) {
        sharedPreferences
            .edit {
                putString(key, value)
            }

    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int, uiElement: PreferenceUi?) {
        sharedPreferences
            .edit {
                putInt(key, value)
            }

    }

}
