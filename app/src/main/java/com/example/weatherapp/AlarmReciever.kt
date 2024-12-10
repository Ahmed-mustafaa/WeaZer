package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
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

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val notificationType = intent?.getStringExtra("NOTIFICATION_TYPE") ?: return
        val alarmTime = intent?.getLongExtra("ALARM_TIME", 0) ?: 0

        Log.d("AlarmReceiver", "Alarm received. Starting WeatherNotificationService.")
        val serviceIntent = Intent(context, WeatherNotificationService::class.java)
        serviceIntent.putExtra("NOTIFICATION_TYPE", notificationType)
        serviceIntent.putExtra("ALARM_TIME", alarmTime)
        // Start the service to handle the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
            context.startForegroundService(serviceIntent) // Use startForegroundService for Android 8.0+
        } else {
            context.startService(serviceIntent)
        }

    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = "Alarm Channel"
            val descriptionText = "Channel for Alarm Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(AlarmScreen.ALARM_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
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
}
