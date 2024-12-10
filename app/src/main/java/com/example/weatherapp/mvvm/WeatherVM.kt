package com.example.weatherapp.mvvm


import SharedPrefs
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherapp.model.ForCast
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherVM(private val repository: WeatherRepository,
                private val sharedPrefsHelper: SharedPrefs
):ViewModel() {
    private val _forecast = MutableLiveData<ForCast?>()
    val forecast: MutableLiveData<ForCast?> = _forecast
    private val _translatedCity = MutableLiveData<String>()
    val translatedCity: LiveData<String> get() = _translatedCity
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
        }
    fun translateCityName(cityName: String) {
        viewModelScope.launch {
            try {
                // Call the function that translates the city name
                val translatedName = repository.translateCityName(cityName)
                Log.i("From weatherVm", "lat: $translatedName")
                _translatedCity.postValue(translatedName) // Update LiveData
            } catch (e: Exception) {
                Log.e("weatherVm", "Error TRANSLATING CITY NAME : ${e.message}")
                e.printStackTrace()
                _translatedCity.postValue("Translation failed")
            }
        }
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
    private fun saveWeatherToCache(weather: ForCast) {
        sharedPrefsHelper.saveWeather(weather)
        Log.i("WeatherVM", "Weather data cached successfully.")
    }

fun getWeatherForLocation(lat: Double, lon: Double, unit: String = "metric"){
    viewModelScope.launch {
        try{
val response = repository.getCurrenWeather(unit)
            Log.i("From weatherVm", "lat: $lat, and lon: $lon")
            _forecast.postValue(response)
        }catch(e:Exception){
            Log.e("weatherVm", "Error fetching weather data: ${e.message}")
            e.printStackTrace()
        }
    }

}
    fun clearCachedWeather() {
        sharedPrefsHelper.clearCachedWeather()
        Log.i("WeatherVM", "Cached weather data cleared.")
    }


    fun getWeatherForCurrentLocation(unit:String="metric") {
        viewModelScope.launch {
            try {
                val response = repository.getCurrenWeather(unit)

                _forecast.postValue(response)
                saveWeatherToCache(response)
            } catch (e: Exception) {
                Log.e("WeatherVM", "Error fetching current weather: ${e.message}")
                e.printStackTrace()
            }

        }
    }
    fun loadCachedWeather() {
        val cachedWeather = sharedPrefsHelper.getCachedWeather()
        if (cachedWeather != null) {
            _forecast.postValue(cachedWeather)
            Log.i("WeatherVM", "Cached weather data loaded.")
        } else {
            Log.i("WeatherVM", "No cached weather data available.")
        }
    }
    }
