package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.AlarmAdapter
import com.example.weatherapp.databinding.AlarmScreenBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmScreen : Fragment() {
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<Alarm>()
    private lateinit var binding: AlarmScreenBinding

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
        private const val ALARM_CHANNEL_ID = "alarm_channel_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlarmScreenBinding.inflate(inflater, container, false)

        val name = arguments?.getString("userName") ?: "No Alarm added"
        binding.noAlarmTextView.text = name
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        checkNotificationPermission()

        binding.alarmRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            alarmAdapter = AlarmAdapter(alarmList)
            adapter = alarmAdapter
        }

        binding.fab.setOnClickListener {
            Toast.makeText(context, "Floating Action Button clicked", Toast.LENGTH_SHORT).show()
            showTimePicker()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val alarmTimeInMillis = getAlarmTimeInMillis(selectedHour, selectedMinute)
            val formattedTime = formatTime(alarmTimeInMillis)

            alarmList.add(Alarm(alarmTimeInMillis, formattedTime))
            alarmAdapter.notifyItemInserted(alarmList.size - 1)
            scheduleAlarm(alarmTimeInMillis)

            Toast.makeText(context, "Alarm set for $formattedTime", Toast.LENGTH_SHORT).show()

        }, hour, minute, true).show()
    }

    private fun formatTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(timeInMillis)
    }

    private fun getAlarmTimeInMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            if (this.get(Calendar.HOUR_OF_DAY) > hour || (this.get(Calendar.HOUR_OF_DAY) == hour && this.get(Calendar.MINUTE) >= minute)) {
                add(Calendar.DAY_OF_YEAR, 1)  // Set for the next day if the time has already passed
            }
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun scheduleAlarm(timeInMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canScheduleExactAlarms()) {
            Toast.makeText(
                context,
                "This app needs permission to schedule alarms. Please enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
            openAppSettings()
            return
        }

        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val uniqueRequestCode = System.currentTimeMillis().toInt()

        val alarmIntent = PendingIntent.getBroadcast(
            requireContext(),
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, alarmIntent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun canScheduleExactAlarms(): Boolean {
        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for alarm notifications"
        }

        val notificationManager = requireContext().getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    data class Alarm(val timeInMillis: Long, val formattedTime: String)
}
