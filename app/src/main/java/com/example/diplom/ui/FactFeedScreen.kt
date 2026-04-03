package com.example.diplom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diplom.data.AiRepository
import com.example.diplom.data.Fact
import com.example.diplom.data.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactFeedScreen(repository: SettingsRepository) {
    val aiRepository = remember { AiRepository(repository) }
    var facts by remember { mutableStateOf(emptyList<Fact>()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val allAvailableTopics = listOf("Все темы", "Наука", "История", "Программирование", "Космос", "Животные", "Искусство", "Психология")
    var selectedTopics by remember { mutableStateOf(repository.getSelectedTopics()) }
    var expanded by remember { mutableStateOf(false) }

    // Оптимизированная начальная загрузка
    LaunchedEffect(selectedTopics) {
        facts = emptyList()
        isLoading = true
        
        // Запускаем 3 запроса параллельно
        val deferredFacts = (1..3).map {
            async { aiRepository.getRandomFact() }
        }
        
        // Ждем завершения всех и фильтруем (если нужно)
        facts = deferredFacts.awaitAll()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Выпадающий список тем
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Темы: ${if (selectedTopics.contains("Все темы")) "Все темы" else selectedTopics.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                allAvailableTopics.forEach { topic ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedTopics.contains(topic),
                                    onCheckedChange = null
                                )
                                Text(text = topic, modifier = Modifier.padding(start = 8.dp))
                            }
                        },
                        onClick = {
                            val newSet = if (topic == "Все темы") {
                                setOf("Все темы")
                            } else {
                                val current = selectedTopics.toMutableSet()
                                current.remove("Все темы")
                                if (current.contains(topic)) current.remove(topic) else current.add(topic)
                                if (current.isEmpty()) setOf("Все темы") else current
                            }
                            selectedTopics = newSet
                            repository.setSelectedTopics(newSet)
                        }
                    )
                }
            }
        }

        if (isLoading && facts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(facts) { fact ->
                    FactCard(fact)
                }
                
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                // Для кнопки "Загрузить еще" тоже можно грузить по 2-3 сразу, если нужно
                                val newFact = aiRepository.getRandomFact()
                                facts = facts + newFact
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Загрузить еще")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FactCard(fact: Fact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = fact.category.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fact.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}