// Package matches location in extension project

package com.example.manglyextension.plugins

import java.util.UUID

abstract class Source(prefs: IPreferences?) {
    val preferences: IPreferences? = prefs

    abstract fun getExtensionName(): String

    abstract fun getPreferenceKey(): UUID

    abstract fun getUiPreferenceKey(): UUID

    abstract fun generateSettings()

}