package com.example.weatherapp.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherapp.model.ForCast
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.model.WeatherList
import com.example.weatherapp.service.Service
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherVM(private val repository: WeatherRepository):ViewModel() {

    private val _forecast = MutableLiveData<ForCast>()
    val forecast: MutableLiveData<ForCast> = _forecast

    private fun handleApiError(code: Int) {
        when (code) {
            400 -> {
                // Handle bad request error
                Log.e("404", "handleApiError: Error, Bad Request ")
            }
            401 -> {
                // Handle unauthorized error
                Log.e("404", "handleApiError: Error, Unauthorized")
            }
            403 -> {
                // Handle forbidden error
                Log.e("404", "handleApiError: Error, Forbidden")
            }
                else -> {
                Log.e("UNK", "handleApiError: Unknown Error ", )
                }
    }

        // method t update location from MapsFragment


        }
    fun updateWeatherFromMap(lat: Double, lon: Double, unit: String = "metric") {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val response = repository.getWeatherByCooridnates(lat, lon, unit)
                Log.i("From weatherVm", "lat: $lat, and lon: $lon")
                _forecast.postValue(response)
            }catch(e:Exception){
                Log.e("weatherVm", "Error fetching weather data: ${e.message}")
                e.printStackTrace()
            }
        }
    }

     fun getWeatherForLocation(lat: Double, lon: Double): ForCast? {
        return try {
            // Make the API call using the repository
            val response = repository.getWeatherByCoordinates(lat, lon, "metric")

            // Check if the response is successful
            if (response.isSuccessful) {
                response.body() // Return the response body if successful
            } else {
                Log.e("WeatherVM", "API error: ${response.code()} - ${response.message()}")
                null // Return null on error
            }
        } catch (e: Exception) {
            Log.e("WeatherVM", "Exception during API call: ${e.message}", e)
            null // Return null on exception
        }
    }

    suspend fun getWeatherByCityName(cityName: String, unit: String): ForCast {
        return repository.getWeatherByCityName(cityName, unit)
    }


    fun getWeatherForCurrentLocation(unit:String="metric") {
        viewModelScope.launch(Dispatchers.IO)  {
            val response = repository.getCurrenWeather(unit)
            try {
                    _forecast.postValue(response)
                    // Handle API error, e.g., show error message or retry


            } catch (e: Exception) {
                // Handle network or other errors
                e.printStackTrace()
            }

        }
    }
    }
