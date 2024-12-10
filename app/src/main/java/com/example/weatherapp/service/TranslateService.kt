package com.example.weatherapp.service
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


data class TranslationRequest(
    val q: String,
    val source: String,
    val target: String,
)



data class TranslationResponse(
    val translatedText: String
)
interface LibreTranslateApi {
    @Headers("Content-Type: application/json")
    @POST("translate")
    suspend fun translate(@Body request: TranslationRequest): TranslationResponse
}