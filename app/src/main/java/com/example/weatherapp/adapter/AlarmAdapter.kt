package com.example.weatherapp.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.View.AlarmScreen
import com.example.weatherapp.R
import java.text.ParseException
import java.text.SimpleDateFormat
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
                val parsedDate = formatter.parse(date) ?: return ""
                val calendar = Calendar.getInstance().apply { time = parsedDate  }
                val todayCalendar = Calendar.getInstance()
                val Tommorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

                if (calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)) {
                    itemView.context.getString(R.string.Today)
                }
                else if (calendar.get(Calendar.YEAR) == Tommorrow.get(Calendar.YEAR)){
                    itemView.context.getString(R.string.Tomorrow)

                }

                else {
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                }

            } catch (e: ParseException) {
                Log.e("AlarmAdapter", "Date parsing error: ${e.message}")
                "" // Or handle the error appropriately
            }
        }

        private fun getTimeOnly(date: String, pattern: String): String {
            return try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                val parsedDate = formatter.parse(date) ?: return "" // Ensure date is not null
                // Format only the time (e.g., 07:00 AM)
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(parsedDate)
            } catch (e: ParseException) {
                Log.e("AlarmAdapter", "Date parsing error: ${e.message}")
                "" // Or handle the error appropriately
            }
        }

        @SuppressLint("SuspiciousIndentation")
        fun bind(alarm: AlarmScreen.Alarm) {
            val inputPattern = "dd/MM/yyyy HH:mm a"
            val day = getDayName(alarm.formattedTime, inputPattern)
            Log.d("AlarmAdapter", "Day: $day")
            alarmTimeTextView.text = day
            val date = getTimeOnly(alarm.formattedTime, inputPattern)
            Log.d("AlarmAdapter", "Date: $date")
            dayText.text = date
            var isAlarmActive: Boolean
            toggleButton.isSelected = alarm.isActive
            toggleButton.setOnClickListener {
                alarm.isActive = !alarm.isActive
                val isAlarmActive = toggleButton.isSelected

                // Animate slider movement
                val endTranslation = if (isAlarmActive) toggleButton.width - toggleSlider.width - 7 else 0
                toggleSlider.animate()
                    .translationX(endTranslation.toFloat()) // Move slider
                    .setDuration(100)

                // Change background based on the alarm state (only after the animation)
                if (isAlarmActive) {
                    toggleSlider.animate().start()
                    toggleSlider.background // Replace with your active background
                } else {
                    toggleSlider.background  = toggleButton.background // Replace with your inactive background
                }

                // Trigger callback to handle alarm setting/canceling
                onToggleChanged(alarm, isAlarmActive)

                // Update the toggle button selected state
                toggleButton.isSelected = isAlarmActive
                }// Trigger the callback function
            }
        }

}