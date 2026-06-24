package com.eren76.manglyextension.plugins

abstract class IPreferences(settingsPreferences: Any?, ctx: Any?) {
    /*
     * Making the variables under here type Any? was an active choice I purposefully made, a choice I might regret later on some might say
     * The reason I have done this is so I can keep these variables as undefined in the extensions project
     * because not all extensions have to rely on android-specific code which is nice when developing a extension as it means you can directly run it rather than having to import it in android first
     */
    var settings: Any? = settingsPreferences // of type SharedPreferences
    var context: Any? = ctx // of type Context

    abstract fun getBoolean(key: String, defaultValue: Boolean): Boolean
    abstract fun setBoolean(key: String, value: Boolean, uiElement: PreferenceUi? = null)

    abstract fun getString(key: String, defaultValue: String): String
    abstract fun setString(key: String, value: String, uiElement: PreferenceUi? = null)

    abstract fun getInt(key: String, defaultValue: Int): Int
    abstract fun setInt(key: String, value: Int, uiElement: PreferenceUi? = null)

}

enum class PreferenceUi {
    TEXTAREA,
    SWITCH
}
