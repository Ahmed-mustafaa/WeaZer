package com.example.weatherapp.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.AlarmScreen
import com.example.weatherapp.FavoritesFragment
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemFavoriteBinding
import com.example.weatherapp.model.FavoriteLocation
import com.example.weatherapp.model.ForCast

class FavAdapter(private val Cities:List<FavoriteLocation>,
                 private val onCityClick: (FavoriteLocation) -> Unit,
                 private val onDeleteClick: (FavoriteLocation) -> Unit
):RecyclerView.Adapter<FavAdapter.FavViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavAdapter.FavViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)

//inflation

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite,parent,false)
        return FavViewHolder(binding)
    }


    override fun onBindViewHolder(holder: FavAdapter.FavViewHolder, position: Int) {
        holder.bind(Cities[position])
    }

    override fun getItemCount(): Int {
        return Cities.size
    }
    inner class FavViewHolder(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root)     {
        fun bind(location: FavoriteLocation){
            cityNameTextView.text= location.cityName
            binding.deleteIcon.setOnClickListener {
                onDeleteClick(location) // Call delete handler
            }
            itemView.setOnClickListener {
                onCityClick(location)
            }

        }
        val cityNameTextView: TextView =itemView.findViewById(R.id.CityName)
        val deleteButton: ImageView =itemView.findViewById(R.id.deleteIcon)

    }
}