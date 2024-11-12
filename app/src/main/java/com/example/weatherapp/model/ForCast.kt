package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class ForCast(
    @SerializedName("cod")
    val cod: String,
    @SerializedName("message")
    val message: Int,
    @SerializedName("cnt")
    val cnt: Int,
    @SerializedName("list")
    val weatherList: List<WeatherList>,
    @SerializedName("city")
    val city: City
)