package com.example.weatherapp.weatherRepository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.weatherapp.Utils
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.service.Service
import com.example.weatherapp.service.TranslationRequest
import com.google.android.gms.location.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class WeatherRepository(private val api: Service, context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val cachedWeatherData = MutableLiveData<ForCast?>()

    /**
     * Fetches the current weather using location or fallback default coordinates.
     */@SuppressLint("MissingPermission")
    suspend fun getCurrenWeather(unit: String): ForCast {
        return withContext(Dispatchers.Main) {
            val locationResult = try {
                getFreshLocation() // Fetch current location
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching location: ${e.message}")
                null // Handle location fetch failure
            }

                // Use fetched location if valid
                Log.d("WeatherRepository", "Fetching weather for location: ${locationResult?.latitude}, ${locationResult?.longitude}")
                api.getCurrentWeather(
                    lat = locationResult?.latitude.toString(),
                    lon = locationResult?.longitude.toString(),
                    units = unit
                )

            }
        }



    /**
     * Fetches weather by latitude and longitude, utilizing cache to minimize redundant calls.
     */
    suspend fun getWeatherWithCache(lat: Double, lon: Double, unit: String): ForCast {
        val cache = cachedWeatherData.value
        if (cache != null) {
            Log.d("WeatherRepository", "Using cached weather data")
            return cache
        }

        Log.d("WeatherRepository", "Fetching weather for coordinates: $lat, $lon")
        val fetchedData = api.getWeatherByCoordinates(lat, lon, unit, Utils.API_KEY)
        cachedWeatherData.postValue(fetchedData)
        return fetchedData
    }

    /**
     * Fetches weather by city name with proper error handling.
     */
    suspend fun getWeatherByCityName(cityName: String, unit: String): ForCast {
        return try {
            Log.d("WeatherRepository", "Fetching weather for city: $cityName")
            api.getWeatherByCityName(cityName, unit, Utils.API_KEY)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather by city name: ${e.message}")
            throw e
        }
    }

    /**
     * Retrieves the latest device location. Fallback to default if not available.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? {
        return withContext(Dispatchers.IO) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000 // Milliseconds
                fastestInterval = 500
            }

            val deferredLocation = CompletableDeferred<Location?>()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (!deferredLocation.isCompleted) {
                        deferredLocation.complete(result.lastLocation)
                    }
                    fusedLocationProviderClient.removeLocationUpdates(this) // Stop updates
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable && !deferredLocation.isCompleted) {
                        deferredLocation.complete(null)
                    }
                }
            }

            try {
                withContext(Dispatchers.Main) {
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Location update error: ${e.message}")
                if (!deferredLocation.isCompleted) {
                    deferredLocation.complete(null)
                }
            }

            val location = deferredLocation.await() // Wait for location result

            if (location == null) {
                Log.e("WeatherRepository", "Unable to fetch location. Returning default.")
            }

            fusedLocationProviderClient.removeLocationUpdates(locationCallback) // Ensure cleanup
            location
        }
    }
    suspend fun translateCityName(cityName: String): String {
        return withContext(Dispatchers.IO) {
            if (cityName.isEmpty()) {
                return@withContext "City name is empty"
            }

            try {
                val request = TranslationRequest(
                    q = cityName,
                    source = "en",
                    target = "ar"
                )

                val response = RetrofitClient.translateApiService.translate(request)
                if (response.translatedText.isNullOrEmpty()) {
                    response.translatedText
                } else {
                    "Translation failed: ${response.translatedText}"
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error translating city name: ${e.message}")
                "Translation failed: ${e.message}"
            }

    }
    }
    suspend  fun getWeatherByCooridnates(lat:Double,lon:Double,unit:String):ForCast {
        return api.getWeatherByCoordinates(lat,lon,unit,Utils.API_KEY)
    }

    /**
     * Stops location updates to prevent memory leaks.
     */
    fun stopLocationUpdates(locationCallback: LocationCallback) {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}
