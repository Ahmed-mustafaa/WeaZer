package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.weatherapp.AlarmScreen.Companion.ALARM_CHANNEL_ID
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WeatherNotificationService : android.app.Service() {
private var weather:String?=null
    private lateinit var isArabic: String
    companion object {
        const val CHANNEL_ID = "AlarmForegroundServiceChannel"
        const val WEATHER_CHANNEL_ID = "WeatherNotificationChannel"
    }


    @SuppressLint("InvalidPeriodicWorkRequestInterval")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        GlobalScope.launch {

            isArabic = isArabicLanguageSelected() ?: "en"
            Log.d("WeatherNotificationService", isArabic)
            val notificationType = intent?.getStringExtra("NOTIFICATION_TYPE") ?: "AUTO"
            val alarmTime = intent?.getLongExtra("ALARM_TIME", 0) ?: 0
            val weatherWorkRequest =
                PeriodicWorkRequest.Builder(WeatherWorker::class.java, 1, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(this@WeatherNotificationService).enqueue(weatherWorkRequest)

            Log.d("WeatherNotificationService", "Notification type: $weather")
            if (notificationType == "MANUAL") {
                weather = fetchWeatherInfo()

                val sharedPref = this@WeatherNotificationService.getSharedPreferences("AlarmLocation", Context.MODE_PRIVATE)
                sharedPref.edit().putString("weather_info", weather.toString()).apply()
               startForeground(2,createNotificationManually(notificationType,weather.toString()))
            }else {
                Log.d("WeatherNotificationService", "Auto notification triggered.")
                // Call fetchWeatherInfo and wait for the result
                val serviceNotification = createNotification("AUTO")
                startForeground(1, serviceNotification)

            }
        }
        return START_NOT_STICKY
    }

    private fun isArabicLanguageSelected(): String? {
        val sharedPreferences = getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("current_locale", "en")
        return languageCode
    }
    @SuppressLint("DefaultLocale")
    private suspend fun fetchWeatherInfo(): String {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = getSharedPreferences("myprefs", Context.MODE_PRIVATE)
                val lat = sharedPreferences.getFloat("latitude", 0.0f)
                val lon = sharedPreferences.getFloat("longitude", 0.0f)
                Log.d("WeatherNotificationService", "Latitude: $lat, Longitude: $lon")
                // Fetch the weather data from the repository
                val weatherRepository =
                    WeatherRepository(RetrofitClient.apiService, applicationContext)

                val weatherResponse = weatherRepository.getWeatherByCooridnates(lat.toDouble(),lon.toDouble() ,Utils.API_KEY)

                Log.d("WeatherNotificationService", "Weather data fetched: ${weatherResponse.city.coord.lat} ${weatherResponse.city.coord.lon}")
                val temp =
                    weatherResponse.weatherList[0].main.temp.minus(273.15).toInt()
                Log.d("WeatherNotificationService", "Temperature: $temp")

                val desc =
                    weatherResponse.weatherList.firstOrNull()?.weather?.firstOrNull()?.description
                val city = weatherResponse.city.name

                Log.d("WeatherNotificationService", "City: $city")
                val weatherInfo =
                    "${getString(R.string.AlarmUpdates)} $temp ${getString(R.string.C)}\n" +
                            if(isArabicLanguageSelected()=="en") ToastUtil.translateWeatherDescription(desc!!,this@WeatherNotificationService) else desc
                sharedPreferences.edit().putString("wInfo", weatherInfo).apply()


                weatherInfo

            } catch (e: Exception) {
                Log.e("WeatherNotificationService", "Error fetching weather data: ${e.message}")
                "Unable to fetch weather data"
            }
        }

    }

    @SuppressLint("ObsoleteSdkInt")
    private fun displayNotification(context: Context, notificationType: String, weatherInfo: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Alarm Notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.weather) // Replace with your app's icon
            .setContentTitle("WeaزZar Alarm")
            .setContentText("$notificationType $weatherInfo")
            .setAutoCancel(false)
            .setSound(alarmSound)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        val notification2 = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.weather) // Replace with your app's icon
            .setContentTitle("WeaزZar Alarm")
            .setContentText(notificationType + " " + weatherInfo.split("\n ").first())
            .setAutoCancel(false)
            .setSound(alarmSound)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Show the notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, if(notificationType=="MANUAL") notification else notification2 )
    }

    @SuppressLint("ObsoleteSdkInt")
    private suspend fun createNotification(notificationType: String): Notification {

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
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val wInfo = fetchWeatherInfo()
        val bigTextStyle = NotificationCompat.BigTextStyle()

            .bigText(wInfo)
        return NotificationCompat.Builder(this, WEATHER_CHANNEL_ID)
                .setColor(Color.parseColor("#D4F6FF"))
                .setColorized(true)
                .setSmallIcon(R.drawable.clouds)
                .setContentTitle(getString(R.string.ThreeHours))
                .setContentText(wInfo)
            .setSound(alarmSound)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

    }
    private  fun createNotificationManually(notificationType: String = "MANUAL", weatherInfo: String): Notification {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEATHER_CHANNEL_ID,
                "Weather Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for weather updates"
                enableVibration(true) // Enable vibration
                vibrationPattern = longArrayOf(0, 1000, 500, 1000) // Custom vibration pattern
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null) // Alarm sound
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
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(weatherInfo)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        return NotificationCompat.Builder(this, WEATHER_CHANNEL_ID)
                .setColor(Color.parseColor("#859F3D"))
                .setColorized(true)
                .setSmallIcon(R.drawable.clouds)
                .setContentTitle(getString(R.string.ManualAlarmUpdate))
                .setSound(alarmSound)
                .setStyle(bigTextStyle)
                .setVibrate(longArrayOf(0, 1000, 500, 1000)) // Vibration pattern
                .setFullScreenIntent(pendingIntent, true) // Makes it behave like an alarm
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

    }

    override fun onBind(intent: Intent?) = null


}
