package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    private lateinit var weatherRepository: WeatherRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        weatherRepository = WeatherRepository(RetrofitClient.apiService, context)

        // Create notification channel for Android 8.0 and above
        createNotificationChannel(context)

        // Fetch weather data and show notification
        fetchWeatherDataForCurrentLocation(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Channel"
            val descriptionText = "Channel for Alarm Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ALARM_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun fetchWeatherDataForCurrentLocation(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the weather data from the repository
                val weatherResponse = weatherRepository.getCurrenWeather(Utils.API_KEY)
                val weatherInfo = weatherResponse.weatherList.firstOrNull()?.main?.feels_like?.toString()
                    ?: "Unable to fetch weather data"

                // Display the weather notification
                displayWeatherNotification(context, weatherInfo)
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to fetch weather data: ${e.message}")
            }
        }
    }

    private fun displayWeatherNotification(context: Context, weatherInfo: String) {
        // Intent to open the main activity when notification is clicked
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setSmallIcon(R.drawable.clouds)
            .setContentTitle("Weather Update")
            .setContentText("Current Weather: $weatherInfo")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Check and request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("AlarmReceiver", "POST_NOTIFICATIONS permission not granted")
            return
        }

        // Show notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
