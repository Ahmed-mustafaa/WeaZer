package com.example.weatherapp.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.format.DateFormat.format
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.AlarmScreen
import com.example.weatherapp.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class AlarmAdapter(private val alarms:Set<AlarmScreen.Alarm>,
                   private val onToggleChanged: (AlarmScreen.Alarm, Boolean) -> Unit

):RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlarmAdapter.AlarmViewHolder {
//inflation

        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item,parent,false)
        return AlarmViewHolder(view)
        }


    override fun onBindViewHolder(holder: AlarmAdapter.AlarmViewHolder, position: Int) {
holder.bind(alarms.elementAt(position))

    }

    override fun getItemCount(): Int {
         return alarms.size
    }
    inner class AlarmViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val alarmTimeTextView: TextView =itemView.findViewById(R.id.formattedTime)
        val dayText:TextView=itemView.findViewById(R.id.Day)
        val toggleButton = itemView.findViewById<FrameLayout>(R.id.toggle_button)
        val toggleSlider = itemView.findViewById<View>(R.id.toggle_slider)

        private fun getDayName(date: String, pattern: String): String {
            return try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                val date = formatter.parse(date)
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            } catch (e: ParseException) {
                Log.e("AlarmAdapter", "Date parsing error: ${e.message}")
                "" // Or handle the error appropriately
            }
        }

        private fun getDate(date: String, pattern: String): String {
            return try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                val date = formatter.parse(date)
                SimpleDateFormat("EEE dd-MM-yyyy-HH:mm a", Locale.getDefault()).format(date)
            } catch (e: ParseException) {
                Log.e("AlarmAdapter", "Date parsing error: ${e.message}")
                "" // Or handle the error appropriately
            }
        }

        @SuppressLint("SuspiciousIndentation")
        fun bind(alarm: AlarmScreen.Alarm) {
            val day = getDayName(alarm.formattedTime, "EEE dd-MM-yyyy-HH:mm a")
            Log.d("AlarmAdapter", "Day: $day")
            alarmTimeTextView.text = day
            val date = getDate(alarm.formattedTime, "EEE dd-MM-yyyy-HH:mm a")
            Log.d("AlarmAdapter", "Date: $date")
            dayText.text = date
            var isAlarmActive: Boolean
            toggleButton.isSelected = alarm.isActive

            toggleButton.setOnClickListener {
                isAlarmActive = toggleButton.isSelected
                // Animate slider movement
                val endTranslation = if (isAlarmActive) toggleButton.width - toggleSlider.width - 8 else 0
                toggleSlider.
                animate()
                    .translationX(endTranslation.toFloat())
                    .setDuration(200)
                    .start()
                if (isAlarmActive) toggleSlider.background
                // Change background state

                    // Trigger callback to handle alarm setting/canceling
                onToggleChanged(alarm, isAlarmActive)
                // Update the toggle button selected state
                toggleButton.isSelected = !isAlarmActive
                }// Trigger the callback function
            }
        }

}