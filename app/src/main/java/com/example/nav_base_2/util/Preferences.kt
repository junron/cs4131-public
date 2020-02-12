package com.example.nav_base_2.util

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

object Preferences {
    lateinit var context: Activity
    lateinit var preferences: SharedPreferences
    fun init(context: Activity) {
        Preferences.context = context
        preferences = context.getPreferences(MODE_PRIVATE)
    }

    fun isDarkMode() = preferences.getBoolean("darkMode?", true)
    fun setDarkMode(darkMode: Boolean) = preferences.edit().putBoolean("darkMode?", darkMode).commit()
}
