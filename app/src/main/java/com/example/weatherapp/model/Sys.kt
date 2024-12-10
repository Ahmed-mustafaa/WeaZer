package com.example.weatherapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class Sys(
    val pod: String
) : Parcelable