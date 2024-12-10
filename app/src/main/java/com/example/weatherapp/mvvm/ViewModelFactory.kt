package com.example.weatherapp.mvvm

import SharedPrefs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.weatherRepository.WeatherRepository

class ViewModelFactory(private val repository: WeatherRepository,private val sharedPrefs: SharedPrefs) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherVM(repository,sharedPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
