package com.example.weatherapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherNotificationService : android.app.Service() {
    private lateinit var weatherRepository: WeatherRepository

    companion object {
        const val CHANNEL_ID = "AlarmForegroundServiceChannel"
        const val WEATHER_CHANNEL_ID = "WeatherNotificationChannel"
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationType = intent?.getStringExtra("NOTIFICATION_TYPE") ?: "AUTO"
        val alarmTime = intent?.getLongExtra("ALARM_TIME", 0) ?: 0
       if (notificationType == "AUTO") {
            Log.d("WeatherNotificationService", "Auto notification triggered.")
            val serviceNotification = createNotification("AUTO")
            startForeground(1, serviceNotification)
        } else {
            Log.d("WeatherNotificationService", "Manual notification triggered.")
           val serviceNotification = createNotification("MANUAL")
           startForeground(1, serviceNotification)        }

        Log.d("WeatherNotificationService", "Service started. Fetching weather data...")
        Log.d("WeatherNotificationService", "Foreground service started. Fetching weather data...")


        weatherRepository = WeatherRepository(RetrofitClient.apiService, applicationContext)
        fetchWeatherData()


        return START_NOT_STICKY
    }


    private fun fetchWeatherData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the weather data
                val weatherResponse = weatherRepository.getCurrenWeather(Utils.API_KEY)
                val weatherInfo = weatherResponse.weatherList.firstOrNull()?.main?.feels_like?.toString()
                    ?: "Unable to fetch weather data"

                // Send notification
/*
                displayWeatherNotification(weatherInfo)
*/
            } catch (e: Exception) {
                Log.e("WeatherNotificationService", "Error fetching weather data: ${e.message}")
            }
            finally {
                stopSelf()

            }
        }
    }



    private fun createNotification(notificationType: String): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEATHER_CHANNEL_ID,
                "Weather Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for weather updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Return the correct notification based on the notification type
        return if (notificationType == "AUTO") {
            NotificationCompat.Builder(this, WEATHER_CHANNEL_ID)
                .setColor(Color.parseColor("#9CA986"))
                .setColorized(true)
                .setSmallIcon(R.drawable.clouds)
                .setContentTitle("Hourly Weather Update")
                .setContentText("It has been 3 hours since the last update ...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        } else {
            NotificationCompat.Builder(this, WEATHER_CHANNEL_ID)
                .setColor(Color.BLACK)
                .setColorized(true)
                .setSmallIcon(R.drawable.clouds)
                .setContentTitle("Weather Update")
                .setContentText("Click here to see your updated weather ...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        }
    }
    override fun onBind(intent: Intent?) = null





}
