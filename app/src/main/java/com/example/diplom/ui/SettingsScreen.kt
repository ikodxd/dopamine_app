package com.example.diplom.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.diplom.data.AppConfig
import com.example.diplom.data.SettingsRepository

@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf(emptyList<AppConfig>()) }
    var selectedPackages by remember { mutableStateOf(repository.getSelectedPackages()) }
    var frequency by remember { mutableIntStateOf(repository.getFrequencyMinutes()) }
    var selectedLanguage by remember { mutableStateOf(repository.getLanguage()) }
    var aiApiKey by remember { mutableStateOf(repository.getAiApiKey()) }

    var showFrequencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context, selectedPackages)
    }

    if (showFrequencyDialog) {
        FrequencyInputDialog(
            currentValue = frequency,
            onDismiss = { showFrequencyDialog = false },
            onConfirm = { newValue ->
                frequency = newValue
                repository.setFrequencyMinutes(newValue)
                showFrequencyDialog = false
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Ввод API ключа
        Text(text = "Gemini API Key:", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = aiApiKey,
            onValueChange = {
                aiApiKey = it
                repository.setAiApiKey(it)
            },
            placeholder = { Text("Введите ваш API ключ здесь") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        )
        
        Text(
            text = "Где найти ключ? Нажмите здесь",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                    context.startActivity(intent)
                }
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Выбор языка
        Text(text = "Язык контента:", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ru" to "Русский", "en" to "English", "de" to "Deutsch", "fr" to "Français").forEach { (code, label) ->
                FilterChip(
                    selected = selectedLanguage == code,
                    onClick = {
                        selectedLanguage = code
                        repository.setLanguage(code)
                    },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionSection(context)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Частота появления: ", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$frequency мин",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable { showFrequencyDialog = true }
            )
            IconButton(onClick = { showFrequencyDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        
        Slider(
            value = frequency.toFloat(),
            onValueChange = { 
                frequency = it.toInt()
                repository.setFrequencyMinutes(frequency)
            },
            valueRange = 1f..120f,
            steps = 119
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Выберите приложения:", style = MaterialTheme.typography.titleMedium)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(installedApps) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedPackages.contains(app.packageName),
                        onCheckedChange = { checked ->
                            val newSet = selectedPackages.toMutableSet()
                            if (checked) newSet.add(app.packageName) else newSet.remove(app.packageName)
                            selectedPackages = newSet
                            repository.setSelectedPackages(newSet)
                        }
                    )
                    Text(text = app.label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun FrequencyInputDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите частоту (мин)") },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val newValue = textValue.toIntOrNull() ?: currentValue
                    onConfirm(newValue.coerceIn(1, 1440))
                }
            ) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun PermissionSection(context: Context) {
    Column {
        Button(onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Разрешить наложение поверх окон")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Включить спец. возможности")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Доступ к статистике использования")
        }
    }
}

private fun getInstalledApps(context: Context, selected: Set<String>): List<AppConfig> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { 
            AppConfig(
                packageName = it.packageName,
                label = pm.getApplicationLabel(it).toString(),
                isSelected = selected.contains(it.packageName)
            )
        }.sortedBy { it.label }
}