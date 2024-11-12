package com.example.weatherapp.adapter

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weatherapp.R
import com.example.weatherapp.model.WeatherList
import java.text.SimpleDateFormat
import java.util.Calendar



class ForeCastAdapter(private val ForeCastWeatherList:List<WeatherList>):RecyclerView.Adapter<ForCastViewHolder>() {


    //the list ( very Importanttttt )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForCastViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.fourdatlist, parent, false)
        return ForCastViewHolder(view)
    }
@RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ForCastViewHolder, position: Int) {
val forecastObject = ForeCastWeatherList[position]
    holder.pressureval.text = forecastObject.main.pressure.toString()
    holder.humidityval.text = forecastObject.main.humidity.toString()
    holder.windSpeedval.text = forecastObject.wind.speed.toString()
    holder.visibilityval.text = forecastObject.visibility.toString()
    Log.d("TAG", "onBindViewHolder: ${forecastObject.visibility}")
    holder.airqualityval.text = forecastObject.main.temp.toString()
    holder.uvval.text = forecastObject.main.temp.toString()
    holder.humidityImage.setImageResource(R.drawable.humidity)
    holder.pressureImage.setImageResource(R.drawable.pressure)
    holder.windImage.setImageResource(R.drawable.wind)
    holder.visibilityImage.setImageResource(R.drawable.visibility)
    holder.AirQualityImage.setImageResource(R.drawable.airquality)
    holder.ulrtraviolentImage.setImageResource(R.drawable.uv)


    }

    override fun getItemCount(): Int {
            return ForeCastWeatherList.size
        }

}
    class ForCastViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val pressureImage: ImageView = itemview.findViewById(R.id.PressureGraphic)
        val windImage: ImageView = itemview.findViewById(R.id.windGraphic)
        val visibilityImage: ImageView = itemview.findViewById(R.id.visibilitygraphic)
        val humidityImage: ImageView = itemview.findViewById(R.id.humiditygraphic)
        val AirQualityImage: ImageView = itemview.findViewById(R.id.airqualitygraphic)
        val humidity: ImageView = itemview.findViewById(R.id.humiditygraphic)
        val ulrtraviolentImage: ImageView = itemview.findViewById(R.id.uvgraphic)

        //values
        val pressureval: TextView = itemview.findViewById(R.id.pressureval)
        val humidityval: TextView = itemview.findViewById(R.id.humidityval)
        val windSpeedval: TextView = itemview.findViewById(R.id.windval)
        val visibilityval: TextView = itemview.findViewById(R.id.visibilityval)
        val airqualityval: TextView = itemview.findViewById(R.id.airqualityval)
        val uvval: TextView = itemview.findViewById(R.id.uvval)

    }

