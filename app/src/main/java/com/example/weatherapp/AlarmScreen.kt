package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.AlarmManager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.AlarmAdapter
import com.example.weatherapp.databinding.AlarmScreenBinding
import com.example.weatherapp.databinding.CustomdialogBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmScreen : Fragment() {
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableSetOf<Alarm>()
    private lateinit var binding: AlarmScreenBinding
    private var isToggled = false

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
        const val ALARM_CHANNEL_ID = "alarm_channel_id"

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlarmScreenBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAlarmsFromSharedPreferences() // Load alarms
        setupRecyclerView()


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                }
            })

        if (alarmList.isEmpty()) {
            binding.noAlarmTextView.visibility = View.VISIBLE
        }
        checkAndRequestPermissions()
        binding.alarmRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            alarmAdapter = AlarmAdapter(alarmList) { alarm, isChecked ->
                if (isChecked) {
                    // Set the alarm
                    scheduleAlarm(alarm.timeInMillis)
                } else {
                    Log.d("AlarmScreen", "Alarm canceled: ${alarm.formattedTime}")
                    cancelAlarm(alarm.timeInMillis)
                }
            }
            adapter = alarmAdapter
        }

        binding.fab.setOnClickListener {
            ToastUtil.showCustomToast(requireContext(), "Floating Action Button clicked")
            showTimePicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    requireContext(),
                    "Notification permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notification permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveAlarmsToSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert the alarm list to JSON
        val gson = com.google.gson.Gson()
        val json = gson.toJson(alarmList)
        editor.putString("alarm_list", json)
        editor.apply()
        if (alarmList.isEmpty()) {
            binding.noAlarmTextView.visibility = View.VISIBLE
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun removeAlarm() {
        val sharedPreferences =
            requireContext().getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = com.google.gson.Gson()
        val json = gson.toJson(alarmList)
        alarmList.clear()
        alarmAdapter.notifyDataSetChanged()
        saveAlarmsToSharedPreferences()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun loadAlarmsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPreferences.getString("alarm_list", null)
        if (!json.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<MutableList<Alarm>>() {}.type
            val loadedAlarms: MutableList<Alarm> = gson.fromJson(json, type)
            alarmList.clear()

            alarmList.addAll(loadedAlarms)
            alarmAdapter = AlarmAdapter(alarmList) { alarm, isEnabled ->
                if (isEnabled) {
                    isToggled = true
                    scheduleAlarm(alarm.timeInMillis)
                } else {
Log.d("AlarmScreen", "Alarm canceled: ${alarm.formattedTime}")
                    cancelAlarm(alarm.timeInMillis,)
                }
            }
            binding.alarmRecyclerView.adapter = alarmAdapter
        }

        if (alarmList.isNotEmpty()) {
            binding.noAlarmTextView.visibility = View.GONE
        } else {
            binding.noAlarmTextView.visibility = View.VISIBLE
        }
    }

    @SuppressLint("BatteryLife")
    private fun promptIgnoreBatteryOptimizations() {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
        // Set an initial minute (e.g., 0)
            .setTitleText("Select Alarm Time") // Optional: Set the title for the picker
            .build()
    picker.addOnPositiveButtonClickListener {

        // Get selected hour and minute from the picker
        val selectedHour = picker.hour
        val selectedMinute = picker.minute
        // Get alarm time in milliseconds (assuming you have a method to calculate this)
        val alarmTimeInMillis = getAlarmTimeInMillis(selectedHour, selectedMinute)

        // Format the time (assuming you have a method to format the time)
        val formattedTime = formatTime(alarmTimeInMillis)
        alarmList.add(Alarm(alarmTimeInMillis, formattedTime))
        alarmAdapter.notifyItemInserted(alarmList.size - 1)
        binding.noAlarmTextView.visibility = View.GONE
        saveAlarmsToSharedPreferences()
        scheduleAlarm(alarmTimeInMillis)
        ToastUtil.showCustomToast(requireContext(), "Alarm set for $formattedTime")
        // Handle the selected time (e.g., update UI or store the alarm time)
        Log.d("MaterialTimePicker", "Selected Time: $formattedTime")

}

        picker.addOnNegativeButtonClickListener {
            // Handle the negative button click (e.g., show a message if no alarms are set)
            if (alarmList.isEmpty()) {
                binding.noAlarmTextView.visibility = View.VISIBLE
            }
        }
        picker.show(childFragmentManager, "MaterialTimePicker")

    }






    private fun formatTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("EEE dd-MM-yyyy-HH:mm a", Locale.getDefault())
        return sdf.format(timeInMillis)
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupRecyclerView() {
        binding.alarmRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            alarmAdapter = AlarmAdapter(alarmList){alarm, isChecked ->
                if (isChecked) {
                    // Set the alarm
                    scheduleAlarm(alarm.timeInMillis)
                } else {
                    // Cancel the alarm
                    cancelAlarm(alarm.timeInMillis)
                }
            }
            adapter = alarmAdapter

            val itemTouchHelperCallback =
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        return false // Drag-and-drop not supported
                    }
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        if (direction == ItemTouchHelper.LEFT) {
                            viewHolder.itemView.animate()
                                .translationX(0f)
                                .setDuration(0)
                                .withEndAction {
                                    // After resetting, show the dialog
                                }.start()

                            setCustomizedDialog(binding.root,viewHolder)

                        }
                    }
                    override fun onChildDraw(
                        c: Canvas,
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        dX: Float,
                        dY: Float,
                        actionState: Int,
                        isCurrentlyActive: Boolean
                    ) {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                    }

                }
            // Attach the ItemTouchHelper to the RecyclerView
            val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun cancelAlarm(timeInMillis: Long) {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        intent.putExtra("NOTIFICATION_TYPE", "MANUAL") // Mark this as a manual alarm
        intent.putExtra("ALARM_TIME", timeInMillis)
        // Use the same uniqueRequestCode that was used when scheduling the alarm
        val uniqueRequestCode = timeInMillis.toInt() // You can store this in a variable or SharedPreferences to reuse it for cancellation
        val alarmIntent = PendingIntent.getBroadcast(
            requireContext(),
            uniqueRequestCode,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Cancel the alarm
        alarmManager.cancel(alarmIntent)
        ToastUtil.showCustomToast(requireContext(), "Alarm canceled!")
    }


    private fun setCustomizedDialog(parent: ViewGroup, viewHolder: RecyclerView.ViewHolder){
        val position = viewHolder.adapterPosition
        val dialogView = CustomdialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView.root)
            .setCancelable(true) // Allow dismissing the dialog by touching outside
            .create()
        dialogView.dialogTitle.text = getString(R.string.alarmDialogConfirmation)
        dialogView.dialogButton.setOnClickListener{
            val newalarmList = alarmList.toMutableList()
            newalarmList.removeAt(position)
            alarmAdapter.notifyItemRemoved(position)
            alarmList.clear()
            alarmList.addAll(newalarmList)
            saveAlarmsToSharedPreferences()

            dialog.dismiss()
        }
        dialogView.CancelButton.setOnClickListener{
            // Reset swipe state
            alarmAdapter.notifyItemChanged(position)
            dialog.dismiss()
        }
        dialog.show()
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
    private fun checkAndRequestPermissions() {
        checkNotificationPermission()
        promptIgnoreBatteryOptimizations()
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleAlarm(timeInMillis: Long) {
        if (!canScheduleExactAlarms()) {
            ToastUtil.showCustomToast(requireContext(), "This app needs permission to schedule alarms. Please enable it in settings.")
            openAppSettings()
            return
        }
        promptIgnoreBatteryOptimizations()
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        intent.putExtra("NOTIFICATION_TYPE", "MANUAL") // Mark this as a manual alarm
        intent.putExtra("ALARM_TIME", timeInMillis)
        val uniqueRequestCode = System.currentTimeMillis().toInt()
        val alarmIntent = PendingIntent.getBroadcast(
            requireContext(),
            uniqueRequestCode,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val currentTime = System.currentTimeMillis()

if(timeInMillis>=currentTime) {
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, alarmIntent)
ToastUtil.showCustomToast(requireContext(), "Alarm scheduled for $timeInMillis!")
}else{
    val nextDayTimeInMillis = timeInMillis + 24 * 60 * 60 * 1000 // Add 24 hours in milliseconds

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextDayTimeInMillis+24*60*60, alarmIntent)
ToastUtil.showCustomToast(requireContext(), "Alarm scheduled for tomorrow at $timeInMillis!")
}
            ToastUtil.showCustomToast(requireContext(), "Alarm scheduled!")


    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarms(): Boolean {
        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private fun openAppSettings() {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!requireContext().packageManager.canRequestPackageInstalls()) {
            if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                 startActivity(intent)
            }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }
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
    override fun onStop() {
        super.onStop()

        // Save the alarms' state before the app is stopped or navigated away
        saveAlarmsToSharedPreferences()

        // Optionally, you can cancel any ongoing alarms if necessary
    }

    data class Alarm(val timeInMillis: Long, val formattedTime: String, var isActive: Boolean = false)
}
