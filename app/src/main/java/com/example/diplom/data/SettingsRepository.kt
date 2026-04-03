package com.example.diplom.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getSelectedPackages(): Set<String> {
        return prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
    }

    fun setSelectedPackages(packages: Set<String>) {
        prefs.edit().putStringSet("selected_packages", packages).apply()
    }

    fun getFrequencyMinutes(): Int {
        return prefs.getInt("frequency_minutes", 30)
    }

    fun setFrequencyMinutes(minutes: Int) {
        prefs.edit().putInt("frequency_minutes", minutes).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("app_language", "ru") ?: "ru"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
    }

    fun getAiApiKey(): String {
        return prefs.getString("ai_api_key", "") ?: ""
    }

    fun setAiApiKey(key: String) {
        prefs.edit().putString("ai_api_key", key).apply()
    }

    fun getSelectedTopics(): Set<String> {
        return prefs.getStringSet("selected_topics", setOf("Все темы")) ?: setOf("Все темы")
    }

    fun setSelectedTopics(topics: Set<String>) {
        prefs.edit().putStringSet("selected_topics", topics).apply()
    }

    fun isDarkTheme(): Boolean? {
        if (!prefs.contains("is_dark_theme")) return null
        return prefs.getBoolean("is_dark_theme", false)
    }

    fun setDarkTheme(isDark: Boolean?) {
        if (isDark == null) {
            prefs.edit().remove("is_dark_theme").apply()
        } else {
            prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        }
    }

    fun getLastInterventionTime(packageName: String): Long {
        return prefs.getLong("last_intervention_$packageName", 0L)
    }

    fun setLastInterventionTime(packageName: String, time: Long) {
        prefs.edit().putLong("last_intervention_$packageName", time).apply()
    }
}