package com.example.weatherapp.model

data class WeatherList(
    var clouds: Clouds,
    var dt: Int? = null,
    var dt_txt: String? = null,
    var main: Main,
    var pop: Double,
    var sys: Sys,
    var visibility: Int,
    var weather: List<Weather>,
    var wind: Wind
)