package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.weatherapp.View.AlarmScreen
import com.example.weatherapp.View.AlarmScreen.Companion.ALARM_CHANNEL_ID
import com.example.weatherapp.View.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("SuspiciousIndentation", "ServiceCast", "WakeLockTimeout")
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null) return
        // Acquire a wake lock to prevent the device from going to sleep
        val sharedPref = context.getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        val weatherInfo = sharedPref.getString("wInfo", "NOWEATHERDATAFROMSHAREDPREFS") ?: "No weather info available"
        val notificationType = intent.getStringExtra("NOTIFICATION_TYPE") ?: "MANUAL"
        val alarmTime = intent.getLongExtra("ALARM_TIME", 0L)
        displayNotification(context, alarmTime, notificationType, weatherInfo)
        Log.d("AlarmReceiver", "Alarm received. Starting WeatherNotificationService.")
        val serviceIntent = Intent(context, WeatherNotificationService::class.java)
        serviceIntent.putExtra("NOTIFICATION_TYPE", notificationType)
        serviceIntent.putExtra("ALARM_TIME", alarmTime)

        // Start the
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent) // Use startForegroundService for Android 8.0+

        } else {
            context.startService(serviceIntent)

        }



    }

        }

    private fun displayNotification(context: Context, alarmTime: Long, type: String, weatherInfo: String)  {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Build the notification
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setColor(Color.parseColor("#859F3D"))
            .setColorized(true)
            .setSmallIcon(R.drawable.clouds)
            .setContentTitle(context.getString(R.string.ManualAlarmUpdate))
            .setSound(alarmSound)
            .setContentText(weatherInfo)
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // Vibration pattern
            .setFullScreenIntent(pendingIntent, true) // Makes it behave like an alarm
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(alarmTime.toInt(), notification)
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtonePlayer = RingtoneManager.getRingtone(context, ringtone)
        ringtonePlayer.play()
        GlobalScope.launch(Dispatchers.Main) {
            delay(5000)
            ringtonePlayer.stop()
        }
    }
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = "Alarm Channel"
            val descriptionText = "Channel for Alarm Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(AlarmScreen.ALARM_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000, 500)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            // Create the notification
            val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL")
                .setSmallIcon(R.drawable.weather) // Replace with your app's icon
                .setContentTitle("Alarm")
                .setContentText("It's time for your scheduled alarm!")
                .setAutoCancel(true)
                .setSound(alarmSound) // Play sound
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Default vibration and lights
                .build()

            // Show the notification
            val uniqueNotificationId = System.currentTimeMillis().toInt() // Unique ID for the notification
            notificationManager.notify(uniqueNotificationId, notification)
        }
        rescheduleAlarm(context)

    }
    @SuppressLint("ScheduleExactAlarm")
     fun rescheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "AUTO")
            putExtra("ALARM_TIME", System.currentTimeMillis())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the next trigger time to 1 minute from now
        val nextTriggerTime = System.currentTimeMillis() + 3 * 60 *60 * 1000 // 3 Hours in milliseconds

        // Reset the alarm using setExactAndAllowWhileIdle
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime,
            pendingIntent
        )
        Log.d("AlarmReceiver", "Alarm rescheduled for 3 hours from Now .")
    }

