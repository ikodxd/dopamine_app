package com.example.diplom.data

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long
)

class UsageRepository(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getUsageStatsForPeriod(selectedPackages: Set<String>, daysAgoStart: Int, daysAgoEnd: Int): List<AppUsageStats> {
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val baseTime = calendar.timeInMillis
        
        val startTime = baseTime - (daysAgoStart * 24 * 60 * 60 * 1000L)
        val endTime = baseTime - (daysAgoEnd * 24 * 60 * 60 * 1000L)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )

        val pm = context.packageManager
        // Группируем по пакету, так как queryUsageStats может вернуть несколько записей для одного приложения
        return stats
            .filter { it.packageName in selectedPackages }
            .groupBy { it.packageName }
            .map { (packageName, usageStatsList) ->
                val totalTime = usageStatsList.sumOf { it.totalTimeInForeground }
                val lastUsed = usageStatsList.maxOf { it.lastTimeStamp }
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }
                AppUsageStats(
                    packageName = packageName,
                    appName = appName,
                    totalTimeInForeground = totalTime,
                    lastTimeUsed = lastUsed
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    fun getTotalScreenTimeForPeriod(daysAgoStart: Int, daysAgoEnd: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis - (daysAgoStart * 24 * 60 * 60 * 1000L)
        val endTime = calendar.timeInMillis - (daysAgoEnd * 24 * 60 * 60 * 1000L)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )
        return stats.sumOf { it.totalTimeInForeground }
    }
}