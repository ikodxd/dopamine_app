package com.example.diplom.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diplom.data.AppUsageStats
import com.example.diplom.data.SettingsRepository
import com.example.diplom.data.UsageRepository
import kotlinx.coroutines.delay

@Composable
fun StatisticsScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val usageRepository = remember { UsageRepository(context) }
    
    var hasPermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var statsCurrent by remember { mutableStateOf(emptyList<AppUsageStats>()) }
    var statsPrevious by remember { mutableStateOf(emptyList<AppUsageStats>()) }
    var totalTimeCurrent by remember { mutableLongStateOf(0L) }
    var totalTimePrevious by remember { mutableLongStateOf(0L) }

    // Обновление статистики в реальном времени (раз в 5 секунд)
    LaunchedEffect(hasPermission) {
        while (hasPermission) {
            val selected = settingsRepository.getSelectedPackages()
            statsCurrent = usageRepository.getUsageStatsForPeriod(selected, 1, 0)
            totalTimeCurrent = usageRepository.getTotalScreenTimeForPeriod(1, 0)
            
            statsPrevious = usageRepository.getUsageStatsForPeriod(selected, 2, 1)
            totalTimePrevious = usageRepository.getTotalScreenTimeForPeriod(2, 1)
            
            delay(5000) // Пауза 5 секунд перед следующим обновлением
        }
    }

    if (!hasPermission) {
        PermissionRequiredView {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Карточка общего времени со сравнением
            TotalTimeCard(totalTimeCurrent, totalTimePrevious)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Использование выбранных приложений:", style = MaterialTheme.typography.titleSmall)
            
            Spacer(modifier = Modifier.height(8.dp))

            if (statsCurrent.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет данных. Откройте выбранные приложения!")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statsCurrent) { currentStat ->
                        val previousStat = statsPrevious.find { it.packageName == currentStat.packageName }
                        UsageCard(currentStat, previousStat)
                    }
                }
            }
        }
    }
}

@Composable
fun TotalTimeCard(current: Long, previous: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Общее время (24ч)", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMillis(current), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            val diff = current - previous
            val percent = if (previous > 0) (diff * 100 / previous) else 0L
            
            Text(
                text = if (diff >= 0) "↑ На ${formatMillis(diff)} больше, чем вчера ($percent%)" 
                       else "↓ На ${formatMillis(-diff)} меньше, чем вчера (${-percent}%)",
                style = MaterialTheme.typography.bodySmall,
                color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun UsageCard(current: AppUsageStats, previous: AppUsageStats?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(current.appName, fontWeight = FontWeight.Bold)
                
                previous?.let {
                    val diff = current.totalTimeInForeground - it.totalTimeInForeground
                    Text(
                        text = if (diff >= 0) "+${formatMillis(diff)}" else "-${formatMillis(-diff)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = formatMillis(current.totalTimeInForeground),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun PermissionRequiredView(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Для статистики нужен доступ к данным об использовании",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantClick) {
            Text("Предоставить доступ")
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}