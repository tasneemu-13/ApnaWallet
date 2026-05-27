package com.example.finfurcate

import android.content.Context

object UserPrefs {
    private const val PREFS_NAME = "finfurcate_user"

    fun save(context: Context, name: String, upi: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("name", name).putString("upi", upi).apply()
    }

    fun getName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("name", "") ?: ""
    }

    fun getUpi(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("upi", "") ?: ""
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // 👇 NEW: DARK MODE MEMORY 👇
    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("dark_mode", false)
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("dark_mode", isDark).apply()
    }
}