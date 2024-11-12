package com.example.weatherapp.adapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.WeatherList
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
class DailyAdapter(private val days: List<WeatherList>) : RecyclerView.Adapter<DailyViewHolder>() {

    // Limit the data to 5 unique days
    private val dailyForecasts = days
        .groupBy { it.dt_txt?.substring(0, 10) } // Group by the date part (yyyy-mm-dd)
        .map { (_, dailyList) -> dailyList[0] }  // Get the first forecast of each day
        .take(5)  // Limit to 5 days

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.daily_item, parent, false)
        return DailyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dailyForecasts.size
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        val weather = dailyForecasts[position]
        val temp = weather.main.temp.toString()
        val weatherDescription = weather.weather[0].description.capitalize()

        // Get the day name (Tomorrow, Monday, etc.)
        val displayDay = when (position) {
            0 ->  holder.itemView.context.getString(R.string.Tomorrow)
            else -> getDayName(weather.dt_txt!!)
        }

        // Set the weather icon and background based on the weather condition
        val icon = weather.weather[0].icon
        setWeatherIcon(holder, icon)
/*
        setBackground(holder.itemView, icon)
*/
        val translatedDescription = translateWeatherDescription(weatherDescription, holder.itemView.context)


        // Display the day and temperature
        holder.tempDisplay.text = "$displayDay - $translatedDescription - ${temp}°C"
    }

    // Method to get the day of the week from a date string
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDayName(date: String): String {
        val forecastDate = date.substring(0, 10) // Extract "yyyy-MM-dd"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = LocalDate.parse(forecastDate, formatter)
        return parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) // e.g., "Monday"
    }

    // Set appropriate weather icon
    private fun setWeatherIcon(holder: DailyViewHolder, icon: String) {
        when (icon) {
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
            else -> holder.imageDisplay.setImageResource(R.drawable.sun) // Default icon
        }
    }
    fun translateWeatherDescription(description: String, context: Context): String {
        return when (description.toLowerCase(Locale.getDefault())) {
            "clear sky" -> context.getString(R.string.clear_sky)
            "few clouds" -> context.getString(R.string.few_clouds)
            "scattered clouds" -> context.getString(R.string.scattered_clouds)
            "overcast clouds" -> context.getString(R.string.overcast_clouds)
            "rain" -> context.getString(R.string.rain)
            "thunderstorm" -> context.getString(R.string.thunderstorm)
            "snow" -> context.getString(R.string.snow)
            "mist" -> context.getString(R.string.mist)
            "smoke" -> context.getString(R.string.smoke)
            "haze" -> context.getString(R.string.haze)
            "dust" -> context.getString(R.string.dust)
            "fog" -> context.getString(R.string.fog)
            "sand" -> context.getString(R.string.sand)
            "ash" -> context.getString(R.string.ash)
            "squall" -> context.getString(R.string.squall)
            "tornado" -> context.getString(R.string.tornado)
            "light rain" -> context.getString(R.string.light_rain)
            "broken clouds"-> context.getString(R.string.broken_clouds)


            else -> description // Return the description as-is if no translation exists
        }
    }

}

class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageDisplay: ImageView = itemView.findViewById(R.id.imageDisp)
    val tempDisplay: TextView = itemView.findViewById(R.id.TempDisp)
}
