package com.example.weatherapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class Wind(
    val deg: Int,
    val gust: Double,
    val speed: Double
) : Parcelable