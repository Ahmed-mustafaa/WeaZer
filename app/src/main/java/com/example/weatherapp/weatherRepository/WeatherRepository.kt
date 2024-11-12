package com.example.weatherapp.weatherRepository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weatherapp.Utils
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.model.WeatherList
import com.example.weatherapp.service.RetrofitClient.Companion.apiService
import com.example.weatherapp.service.Service
import com.google.android.gms.location.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response


class WeatherRepository(val api:Service,context: Context) {

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission")
   suspend fun getCurrenWeather(unit:String): ForCast{
       return withContext(Dispatchers.IO){
           val locationResult = getFreshLocation()
           locationResult?.let{ location ->
               api.getCurrentWeather(
                   lat =  location.latitude.toString(),
                   lon =  location.longitude.toString(),
                   units = unit)
           }

               ?:throw Exception("Unable to fetch location")


           }
       }
@SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? {
        return withContext(Dispatchers.IO) {
            val locationRequest = LocationRequest.Builder(100)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            val deferredLocation = CompletableDeferred<Location?>()

            var locationResult: Location? = null

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    locationResult = result.lastLocation
                    deferredLocation.complete(locationResult)

                }
                    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                        if (!locationAvailability.isLocationAvailable) {
                            deferredLocation.complete(null)
                        }
                    }
            },    Looper.getMainLooper())

            return@withContext  deferredLocation.await()
        }
    }

  suspend  fun getWeatherByCooridnates(lat:Double,lon:Double,unit:String):ForCast {
        return api.getWeatherByCoordinates(lat,lon,unit,Utils.API_KEY)
    }
     fun getWeatherByCoordinates(lat: Double, lon: Double, unit: String): Response<ForCast> {
        return api.getWeather(lat, lon, unit, apiKey = Utils.API_KEY) // Use your API key if needed
    }

    suspend fun getWeatherByCityName(cityName: String, unit: String): ForCast {
        return api.getWeatherByCityName(
            cityName,
            "metric",
            Utils.API_KEY
        )// execute() for synchronous call
    }
    }


