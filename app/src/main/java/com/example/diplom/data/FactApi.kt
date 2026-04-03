package com.example.diplom.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FactApi {
    // Используем публичный API (например, API Ninja или подобный)
    // Для примера возьмем один из доступных эндпоинтов
    @GET("v1/facts")
    suspend fun getFacts(
        @Query("limit") limit: Int = 1
    ): List<FactResponse>
}

data class FactResponse(
    val fact: String
)