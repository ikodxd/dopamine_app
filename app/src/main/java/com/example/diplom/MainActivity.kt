package com.example.diplom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.example.diplom.data.SettingsRepository
import com.example.diplom.ui.MainNavigation
import com.example.diplom.ui.theme.DiplomTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = SettingsRepository(this)
        
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by remember { 
                mutableStateOf(repository.isDarkTheme() ?: systemDark) 
            }

            DiplomTheme(darkTheme = isDark) {
                MainNavigation(
                    repository = repository,
                    isDark = isDark,
                    onThemeToggle = {
                        val newValue = !isDark
                        isDark = newValue
                        repository.setDarkTheme(newValue)
                    }
                )
            }
        }
    }
}