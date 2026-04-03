package com.example.diplom.service

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import com.example.diplom.data.SettingsRepository

class AppDetectionService : AccessibilityService() {

    private lateinit var repository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Если мы уже показываем оверлей, не нужно реагировать
            if (packageName == this.packageName) return

            val selectedPackages = repository.getSelectedPackages()
            if (selectedPackages.contains(packageName)) {
                checkAndShowOverlay(packageName)
            }
        }
    }

    private fun checkAndShowOverlay(packageName: String) {
        val lastTime = repository.getLastInterventionTime(packageName)
        val frequencyMillis = repository.getFrequencyMinutes() * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime >= frequencyMillis) {
            Log.d("AppDetectionService", "Triggering overlay for: $packageName")
            
            // Сворачиваем приложение (имитируем нажатие кнопки Home)
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // Ждем долю секунды и запускаем оверлей
            repository.setLastInterventionTime(packageName, currentTime)
            
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra("target_package", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startService(intent)
        }
    }

    override fun onInterrupt() {}
}