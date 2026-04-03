package com.example.diplom.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository(private val settings: SettingsRepository) {

    private fun getModel(): GenerativeModel {
        val key = settings.getAiApiKey()
        
        // Добавляем конфигурацию для стабильности JSON
        val config = generationConfig {
            responseMimeType = "application/json"
        }

        return GenerativeModel(
            // Используем gemini-1.5-flash-latest для лучшей совместимости
            modelName = "gemini-1.5-flash-latest",
            apiKey = key,
            generationConfig = config
        )
    }

    suspend fun getRandomFact(): Fact = withContext(Dispatchers.IO) {
        val targetLang = settings.getLanguage()
        val selectedTopics = settings.getSelectedTopics()
        
        val topicsConstraint = if (selectedTopics.contains("Все темы") || selectedTopics.isEmpty()) {
            "on any interesting topic"
        } else {
            "strictly related to one of the following topics: ${selectedTopics.joinToString(", ")}"
        }

        val prompt = """
            Generate one unique and highly interesting fact $topicsConstraint. 
            Format the output as a JSON object with two fields: 
            "category" (short category name) and 
            "text" (the fact itself).
            Language: $targetLang.
        """.trimIndent()

        val key = settings.getAiApiKey()
        if (key.isBlank()) {
            return@withContext Fact(0, "API ключ не установлен.", "Ошибка")
        }

        try {
            val response = getModel().generateContent(prompt)
            val jsonText = response.text?.trim() ?: throw Exception("Empty response")
            
            // Очистка от markdown (на всякий случай)
            val cleanJson = jsonText.removeSurrounding("```json", "```").trim()

            // Парсинг
            val category = cleanJson.substringAfter("\"category\":").substringBefore(",").replace("\"", "").replace(":", "").trim()
            val text = cleanJson.substringAfter("\"text\":").substringBeforeLast("\"").substringAfterLast("\"").trim()
            
            // Если парсинг выше слишком хрупкий, можно использовать упрощенный:
            val finalCategory = if (category.length > 30) "Факт" else category
            val finalText = if (cleanJson.contains("\"text\":")) {
                cleanJson.substringAfter("\"text\":").substringAfter("\"").substringBeforeLast("\"")
            } else cleanJson

            Fact(
                id = (0..10000).random(),
                text = finalText,
                category = finalCategory
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.message ?: ""
            val errorMessage = when {
                msg.contains("404") -> "Модель не найдена. Проверьте API ключ и используйте актуальную модель."
                msg.contains("403") -> "Ошибка доступа. Проверьте API ключ и включите VPN."
                else -> "Ошибка ИИ: ${e.localizedMessage}"
            }
            Fact(0, errorMessage, "Ошибка")
        }
    }
}