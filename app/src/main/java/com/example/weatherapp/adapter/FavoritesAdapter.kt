package com.example.weatherapp.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.ForCast
import com.google.android.material.circularreveal.cardview.CircularRevealCardView
import java.util.Calendar

class FavoritesAdapter(private var favoriteItemsList:List<ForCast>,
                       private val context: Context

):RecyclerView.Adapter<FavoritesViewHolder>() {
    private val DAY_START_HOUR = 6
    private val DAY_END_HOUR = 18

    //the list ( very Importanttttt ) 34an a5od mnha data bta3t el weather of the city
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return FavoritesViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val weather = favoriteItemsList[position]
        holder.cityName.text = weather.city.name
        holder.DisplayTemp.text = "${weather.weatherList[0].main.temp.toInt()}°C"
        val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val cityNameFromSharedPrefs = sharedPreferences.getString("fullLocation", null)
        val cityName = cityNameFromSharedPrefs ?: weather.city.name
        holder.cityName.text = cityName
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val isDayTime = hourOfDay in DAY_START_HOUR..DAY_END_HOUR
        val isClearSky = weather.weatherList[0].weather[0].description.contains("clear sky")

        holder.favoriteItemsImage.setBackgroundResource(
            if (isClearSky && isDayTime) R.drawable.clearskybackground
            else R.drawable.clearskynight)
    }


    override fun getItemCount(): Int {
        return favoriteItemsList.size
    }

    fun updateData(newData: List<ForCast>) {
        Log.d("FavoritesAdapter", "Updating adapter with new data: $newData")
        favoriteItemsList = newData
        notifyDataSetChanged() // Notify the adapter to refresh
    }
}
class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val favoriteItemsImage: CircularRevealCardView = itemView.findViewById(R.id.favItem)
    val cityName: TextView = itemView.findViewById(R.id.CityName)
    val DisplayTemp: TextView = itemView.findViewById(R.id.DisplayTemp)


}
