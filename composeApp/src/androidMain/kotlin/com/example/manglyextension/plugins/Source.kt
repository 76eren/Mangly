// Package matches location in extension project

package com.example.manglyextension.plugins

abstract class Source(prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    abstract fun getExtensionName(): String


    data class SettingGen(
        var key: String,
        var defaultValue: Any,
        var uiElement: PreferenceUi? = null
    )
    abstract fun generateSettings(): List<SettingGen>

}