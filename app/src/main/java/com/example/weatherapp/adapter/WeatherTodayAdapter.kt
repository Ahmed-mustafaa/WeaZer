package com.example.weatherapp.adapter


import android.annotation.SuppressLint
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
import java.util.Calendar
import java.util.Locale

class WeatherTodayAdapter(private val hourlyForecast: List<WeatherList>):RecyclerView.Adapter<TodayViewHolder>() {
    //the list ( very Important )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_fore_cast, parent, false)
        return TodayViewHolder(view)
    }

    override fun getItemCount(): Int {
        return hourlyForecast.size
        Log.i("itemcount", hourlyForecast.size.toString())
    }
    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TodayViewHolder, position: Int) {
        val weather = hourlyForecast[position]

        val temp = weather.main.temp
        Log.i("temp", temp.toString())
        holder.tempDisplay.text = String.format(" °C", temp)
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
        Log.i("time", holder.timeDisplay.text.toString())

        // Reformat time
        // take Calendar object
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
/*
        watherTime= String.format(dateFormat.format(calendar.time))
*/

        val time = dateFormat.format(calendar.time)

        val timeofapi = weather.dt_txt!!.split("-")


        Log.i("timeofapi", timeofapi.toString())
        val partbeforecolon = timeofapi[0]
        val partafterspace = timeofapi[1]
/*
        holder.timeDisplay.text = timeofapi
*/

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
}

class TodayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageDisplay: ImageView = itemView.findViewById(R.id.imageDisplay)
    val tempDisplay: TextView = itemView.findViewById(R.id.TempDisplay)
    val timeDisplay: TextView = itemView.findViewById(R.id.TimeDisplay)
}
