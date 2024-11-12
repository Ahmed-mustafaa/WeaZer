package com.example.weatherapp.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.weatherRepository.WeatherRepository

class ViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherVM(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
