package com.example.diplom.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiRepository(private val settings: SettingsRepository) {

    private val translateRetrofit = Retrofit.Builder()
        .baseUrl("https://api.mymemory.translated.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val translateApi = translateRetrofit.create(TranslationApi::class.java)

    private fun getGenerativeModel(): GenerativeModel {
        val userApiKey = settings.getAiApiKey().trim()
        val config = generationConfig {
            responseMimeType = "application/json"
        }

        return GenerativeModel(

            modelName = "gemini-2.5-flash",
            apiKey = userApiKey,
            generationConfig = config
        )
    }

    suspend fun getRandomFacts(count: Int = 5): List<Fact> = withContext(Dispatchers.IO) {
        val apiKey = settings.getAiApiKey().trim()
        if (apiKey.isBlank()) {
            return@withContext listOf(Fact(0, "API ключ Gemini не установлен.", "Настройка"))
        }

        val targetLang = settings.getLanguage()
        val selectedTopics = settings.getSelectedTopics()
        
        val topicsQuery = if (selectedTopics.contains("Все темы") || selectedTopics.isEmpty()) {
            "on various scientific topics"
        } else {
            "strictly about: ${selectedTopics.joinToString(", ")}"
        }

        val prompt = """
            Generate $count unique, short and surprising scientific facts $topicsQuery.
            Return ONLY a valid JSON array of objects:
            [
              {
                "category": "Topic Name",
                "text": "The fact content in English"
              }
            ]
        """.trimIndent()

        try {
            val result = getGenerativeModel().generateContent(prompt)
            val jsonResponse = result.text?.trim() ?: throw Exception("Empty AI response")
            
            val cleanJson = jsonResponse.removeSurrounding("```json", "```").trim()
            val jsonArray = JSONArray(cleanJson)
            
            val facts = mutableListOf<Fact>()

            val deferredFacts = (0 until jsonArray.length()).map { i ->
                async {
                    val obj = jsonArray.getJSONObject(i)
                    val englishCategory = obj.optString("category", "Science")
                    val englishText = obj.optString("text", "")

                    if (englishText.isNotBlank()) {
                        val (finalText, finalCategory) = if (targetLang != "en") {
                            translateText(englishText, "en", targetLang) to 
                            translateText(englishCategory, "en", targetLang)
                        } else {
                            englishText to englishCategory
                        }

                        Fact(
                            id = (0..999999).random(),
                            text = finalText,
                            category = finalCategory
                        )
                    } else null
                }
            }

            facts.addAll(deferredFacts.awaitAll().filterNotNull())
            facts
        } catch (e: Exception) {
            Log.e("AiRepository", "Request error detail", e)
            listOf(handleError(e))
        }
    }

    suspend fun getRandomFact(): Fact {
        return getRandomFacts(1).firstOrNull() ?: Fact(0, "Не удалось загрузить факт", "Ошибка")
    }

    private suspend fun translateText(text: String, from: String, to: String): String {
        return try {
            val response = translateApi.translate(text, "$from|$to")
            response.responseData.translatedText
        } catch (e: Exception) {
            text
        }
    }

    private fun handleError(e: Exception): Fact {
        val msg = e.message ?: ""

        val userFriendlyError = when {
            msg.contains("404") -> "Модель 'gemini-1.5-flash' не найдена (404). Проверьте: 1) Включен ли VPN (даже если регион доступен, API может блокироваться). 2) Правильность API ключа в настройках."
            msg.contains("403") -> "Ошибка 403: Доступ запрещен. Ваш ключ может быть не активен или лимиты исчерпаны."
            else -> "Ошибка: ${e.localizedMessage}"
        }
        return Fact(0, userFriendlyError, "Ошибка")
    }
}