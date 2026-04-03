package com.example.diplom.data

data class AppConfig(
    val packageName: String,
    val label: String,
    val isSelected: Boolean = false
)

data class UserPreferences(
    val selectedPackages: Set<String> = emptySet(),
    val frequencyMinutes: Int = 30 // Default frequency
)