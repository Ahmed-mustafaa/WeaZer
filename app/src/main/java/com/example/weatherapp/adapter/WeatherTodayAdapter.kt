package com.example.weatherapp.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.WeatherList
import org.intellij.lang.annotations.Language
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class WeatherTodayAdapter(private val hourlyForecast: List<WeatherList>, var selectedUnit: String ):RecyclerView.Adapter<TodayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_fore_cast, parent, false)
        return TodayViewHolder(view)
    }
    fun updateUnit(newUnit: String) {
        selectedUnit = newUnit
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return hourlyForecast.size
    }
    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: TodayViewHolder, position: Int) {
        val weather = hourlyForecast[position]
        val Language = holder.itemView.context.getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val sharedPrefs = holder.itemView.context.getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val currentLanguage = Locale.getDefault()
        val temp = weather.main.temp.roundToInt()
        Log.i("temp", temp.toString())
        if (currentLanguage == Locale("ar")) {
            holder.tempDisplay.text = when (selectedUnit) {
                // Kelvin to Celsius
                "Fahrenheit" -> formatNumberToArabic((temp * 9 / 5) + 32) // celisus  to Fahrenheit
                "Kelvin" -> formatNumberToArabic((temp - 32) * 5 / 9 + 273.15.toInt()) // Kelvin remains Kelvin
                else -> formatNumberToArabic(temp) // Default to Kelvin
            }
        } else {
            holder.tempDisplay.text = when (selectedUnit) {
                "Fahrenheit" -> "${(temp * 9 / 5) + 32}°F" // celisus  to Fahrenheit
                "Kelvin" -> "${(temp - 32) * 5 / 9 + 273.15}°K" // Kelvin remains Kelvin
                else -> "$temp °C" // Default to Kelvin
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "isArabic") {
                val currentLanguage = sharedPrefs.getString("isArabic", "en")
                Log.i("currentLanguage", currentLanguage.toString())
                val temp = weather.main.temp.roundToInt()
                Log.i("temp", temp.toString())
                if (currentLanguage=="ar") {
                    holder.tempDisplay.text = when (selectedUnit) {
                        // Kelvin to Celsius
                        "Fahrenheit" -> formatNumberToArabic((temp * 9 / 5) + 32) // celisus  to Fahrenheit
                        "Kelvin" -> formatNumberToArabic((temp - 32) * 5 / 9 + 273.15.toInt()) // Kelvin remains Kelvin
                        else -> formatNumberToArabic(temp) // Default to Kelvin
                    }
                } else {
                    Log.i("LocalFromElseListening", currentLanguage.toString())

                    holder.tempDisplay.text = when (selectedUnit) {
                        "Fahrenheit" -> "${(temp * 9 / 5) + 32}°F" // celisus  to Fahrenheit
                        "Kelvin" -> "${(temp - 32) * 5 / 9 + 273.15}°K" // Kelvin remains Kelvin
                        else -> "$temp °C" // Default to Kelvin
                    }
                }
            }
        }

        var watherTime =  weather.dt_txt!!.subSequence(11, 16).toString()
        val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour format
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12
        try {
            // Parse and format the time
            val date = inputFormat.parse(watherTime)
            val watherTime12 = outputFormat.format(date)
            holder.timeDisplay.text = watherTime12
        } catch (e: Exception) {
            e.printStackTrace()
            holder.timeDisplay.text = watherTime // Fallback to 24-hour if conversion fails
        }

        when (weather.weather[0].icon) {
            "01d" -> holder.imageDisplay.setImageResource(R.drawable.sun)
            "01n" -> holder.imageDisplay.setImageResource(R.drawable.moon)
            "02d" -> holder.imageDisplay.setImageResource(R.drawable.clouds)
            "02n" -> holder.imageDisplay.setImageResource(R.drawable.towon)
            "03d", "03n" -> holder.imageDisplay.setImageResource(R.drawable.plaincouds)
            "04d", "04n" -> holder.imageDisplay.setImageResource(R.drawable.four)
            "09d", "09n" -> holder.imageDisplay.setImageResource(R.drawable.rainy)
            "10d", "10n" -> holder.imageDisplay.setImageResource(R.drawable.cloudy)
            "11d", "11n" -> holder.imageDisplay.setImageResource(R.drawable.thunder)
            "13d", "13n" -> holder.imageDisplay.setImageResource(R.drawable.snowflake)
            else -> holder.imageDisplay.setImageResource(R.drawable.sun) // A default icon
        }
    }
    fun formatNumberToArabic(number: Int): String {
        return number.toString().map { char ->
            when (char) {
                '0' -> '٠'
                '1' -> '١'
                '2' -> '٢'
                '3' -> '٣'
                '4' -> '٤'
                '5' -> '٥'
                '6' -> '٦'
                '7' -> '٧'
                '8' -> '٨'
                '9' -> '٩'
                else -> char // Return the character as is if it's not a digit
            }
        }.joinToString("")
    }
}

class TodayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageDisplay: ImageView = itemView.findViewById(R.id.imageDisplay)
    val tempDisplay: TextView = itemView.findViewById(R.id.TempDisplay)
    val timeDisplay: TextView = itemView.findViewById(R.id.TimeDisplay)
}
