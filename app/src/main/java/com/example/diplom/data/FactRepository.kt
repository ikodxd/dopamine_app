package com.example.diplom.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface OpenFactApi {
    @GET("api/v2/facts/random?language=en")
    suspend fun getRandomFact(): OpenFactResponse
}

data class OpenFactResponse(
    val text: String
)

class FactRepository(private val settings: SettingsRepository) {
    
    private val factRetrofit = Retrofit.Builder()
        .baseUrl("https://uselessfacts.jsph.pl/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val translateRetrofit = Retrofit.Builder()
        .baseUrl("https://api.mymemory.translated.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val factApi = factRetrofit.create(OpenFactApi::class.java)
    private val translateApi = translateRetrofit.create(TranslationApi::class.java)

    suspend fun getRandomFact(): Fact {
        return try {
            val factResponse = factApi.getRandomFact()
            val targetLang = settings.getLanguage()
            
            val finalBody = if (targetLang != "en") {
                translateText(factResponse.text, "en", targetLang)
            } else {
                factResponse.text
            }

            Fact(
                id = (0..1000).random(),
                text = finalBody,
                category = if (targetLang == "ru") "Интересно" else "Fact"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Fact(0, "Error loading fact", "Error")
        }
    }

    private suspend fun translateText(text: String, from: String, to: String): String {
        return try {
            val response = translateApi.translate(text, "$from|$to")
            response.responseData.translatedText
        } catch (e: Exception) {
            text // Возвращаем оригинал при ошибке
        }
    }
}