package com.example.weatherapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.utils.Utils
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the weather data
                val weatherRepository = WeatherRepository(RetrofitClient.apiService, applicationContext)
                val weatherResponse = weatherRepository.getCurrenWeather(Utils.API_KEY)
                val weatherInfo = weatherResponse.weatherList.firstOrNull()?.main?.temp?.toString()
                    ?: "Unable to fetch weather data"

                // Process or store the weather data as needed (e.g., in SharedPreferences or Database)

                // Return success once done
                Result.success()
            } catch (e: Exception) {
                // Log the error if any
                Result.failure()
            }
        }
    }

}