package com.example.diplom.data

import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationApi {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String
    ): TranslationResponse
}

data class TranslationResponse(
    val responseData: TranslationData
)

data class TranslationData(
    val translatedText: String
)