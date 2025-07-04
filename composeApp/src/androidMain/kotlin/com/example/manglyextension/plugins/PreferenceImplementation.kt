package com.example.manglyextension.plugins

import android.content.Context

class PreferenceImplementation(sharedPreferences: Any?, uiSharedPreferences: Any?, context: Any?) : IPreferences(sharedPreferences, uiSharedPreferences, context) {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return defaultValue
    }

    override fun setBoolean(key: String, value: Boolean, uiElement: PreferenceUi?) {
        TODO("Not yet implemented")
    }


    override fun getString(key: String, defaultValue: String): String {
        return defaultValue
    }

    override fun setString(key: String, value: String, uiElement: PreferenceUi?) {
        TODO("Not yet implemented")
    }


    override fun getInt(key: String, defaultValue: Int): Int {
        return defaultValue
    }

    override fun setInt(key: String, value: Int, uiElement: PreferenceUi?) {
        TODO("Not yet implemented")
    }


    override fun bindKeyToUiElement(key: String, element: PreferenceUi) {

    }

}