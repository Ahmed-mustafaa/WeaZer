package com.example.weatherapp.service

import com.example.weatherapp.Utils
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.model.WeatherList
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface Service {
    @GET("forecast?")
    suspend  fun getCurrentWeather(
        @Query("lat")
        lat:String,
        @Query("lon")
        lon:String,
        @Query("units")
        units:String,
        @Query("appid")
        appid:String =Utils.API_KEY
    ): ForCast
    @GET("forecast?")
    suspend fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") unit: String,
        @Query("appid") apiKey: String // Make sure to include your API key
    ): ForCast
    @GET("forecast?")
     fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") unit: String,
        @Query("appid") apiKey: String // Make sure to include your API key
    ):Response<ForCast>

    // Update this with your API endpoint
    fun getWeatherByCityName(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        // Add your API key here
        @Query("units") unit: String,
    ): ForCast
    @GET("forecast?")
    fun getWeatherByCity(
       @Query("q")
       q:String,
     @Query("appid")
     appid:String =Utils.API_KEY
    ): ForCast
}