package com.example.weatherapp.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemFavoriteBinding
import com.example.weatherapp.model.ForCast

class FavAdapter(private val Cities: MutableList<ForCast>,
                 private val onCityClick: (ForCast) -> Unit,
                 private val onDeleteClick: (ForCast) -> Unit
):RecyclerView.Adapter<FavAdapter.FavViewHolder>() {
    private val DAY_START_HOUR = 6
    private val DAY_END_HOUR = 18
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavAdapter.FavViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)

//inflation

        return FavViewHolder(binding)
    }


    override fun onBindViewHolder(holder: FavAdapter.FavViewHolder, position: Int) {
        holder.bind(Cities[position],position)
        val weather = Cities[position]
        Log.i("weather",weather.weatherList[position].dt_txt.toString())

    }

    override fun getItemCount(): Int {
        return Cities.size
    }
    inner class FavViewHolder(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root)     {
        fun bind(location: ForCast?, position: Int){
            cityNameTextView.text= location?.city?.name
            DisplayTemp.text="${location?.weatherList?.get(position)?.main?.temp}°C"
            location?.weatherList?.get(position)?.dt_txt?.let { Log.d("Timestamp", it) }


            val Timing =location?.weatherList?.get(position)?.dt_txt?.substring(11,13)?.toIntOrNull()
            Time.text =Timing?.let {
                val isPm=it >=12
                val normalHour=if(it%12==0) 12 else it %12
                val amPm= if(isPm) "PM" else "AM"
                "$normalHour:00 $amPm"
            }?:"N/A"
            binding.deleteIcon.setOnClickListener {
                if (location != null) {
                    onDeleteClick(location)
                } // Call delete handler
            }
            itemView.setOnClickListener {
                if (location != null) {
                    onCityClick(location)
                }
            }

        }
        val cityNameTextView: TextView =itemView.findViewById(R.id.CityName)
        val deleteButton: ImageView =itemView.findViewById(R.id.deleteIcon)
        val DisplayTemp :TextView=(itemView.findViewById(R.id.DisplayTemp))
        val Time:TextView = itemView.findViewById(R.id.Time)

    }
}