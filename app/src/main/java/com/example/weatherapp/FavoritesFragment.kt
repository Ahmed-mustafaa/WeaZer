package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.FavAdapter
import com.example.weatherapp.adapter.FavoritesAdapter
import com.example.weatherapp.databinding.ActivityFavoritesBinding
import com.example.weatherapp.model.FavoriteLocation
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.launch
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class FavoritesFragment : Fragment() {
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var weatherVM: WeatherVM
    private lateinit var favoritesAdapter: FavAdapter
    private val favoriteWeatherList = mutableListOf<FavoriteLocation>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = WeatherRepository(RetrofitClient.apiService, requireActivity())
        val viewModelFactory = ViewModelFactory(repository)
        weatherVM = ViewModelProvider(requireActivity(), viewModelFactory)[WeatherVM::class.java]
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("locationResult")
            ?.observe(viewLifecycleOwner) { bundle ->
                val cityName = bundle.getString("cityName", "Unknown City")
                val latitude = bundle.getDouble("latitude", 0.0)
                val longitude = bundle.getDouble("longitude", 0.0)
                Log.d("FavoritesFragment", "CityName: $cityName")
                // Now, handle the retrieved data as needed
                updateFavoritesWithCity(cityName, latitude, longitude)

            }


        loadFavoriteCities()

        // Floating Action Button for navigating to MapsFragment
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.mapsFragment)
        }
    }
    // Load favorite cities from Shared Preferences
    private fun loadFavoriteCities() {
        val sharedPrefs = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        val favoriteCitiesJson = sharedPrefs.getString("favoriteWeatherList", null)
        favoriteWeatherList.clear()

        if (!favoriteCitiesJson.isNullOrEmpty()) {
            favoriteWeatherList.clear()
            val loadedCities =
                Gson().fromJson(favoriteCitiesJson, Array<FavoriteLocation>::class.java).toList()
            favoriteWeatherList.addAll(loadedCities)
            binding.noFav.visibility = View.GONE // Hide "No favorites" message
            favoritesAdapter = FavAdapter(
                favoriteWeatherList,
                { favoriteLocation ->
                    GoToMainWithUpdatedLocation(favoriteLocation)
                },
                { favoriteLocation ->
                    removeCityFromFavorites(favoriteLocation)
                },
            )


            binding.favoritesRV.adapter = favoritesAdapter
            binding.favoritesRV.layoutManager = LinearLayoutManager(context)
            binding.noFav.visibility = View.GONE // Hide "No favorites" message
            binding.favoritesRV.visibility = View.VISIBLE // Show RecyclerView
        } else {
            binding.noFav.visibility = View.VISIBLE // Show "No places added" message
            binding.favoritesRV.visibility = View.GONE // Hide RecyclerView
        }
    }
    private fun GoToMainWithUpdatedLocation(favoriteLocation: FavoriteLocation) {
       val Intent = Intent(requireContext(), MainActivity::class.java)
        Intent.putExtra("latitude", favoriteLocation.latitude)
        Intent.putExtra("longitude", favoriteLocation.longitude)
        startActivity(Intent)
    }
    private fun removeCityFromFavorites(favoriteLocation: FavoriteLocation) {
        // Remove city from the list
        favoriteWeatherList.remove(favoriteLocation)
        favoritesAdapter.notifyDataSetChanged() // Notify the adapter about the change

        // Save the updated favorites list to SharedPreferences
        saveFavorites()

        // Show a toast confirming the deletion
        showToast("${favoriteLocation.cityName} removed from favorites.")
    }
    private fun updateFavoritesWithCity(cityName: String, latitude: Double, longitude: Double) {
      val alreadyFavorited =
          favoriteWeatherList.any { it.cityName == cityName && it.latitude == latitude && it.longitude == longitude }

      if (!alreadyFavorited) {
          lifecycleScope.launch {
              val newFavorite = FavoriteLocation(cityName, latitude, longitude)
              favoriteWeatherList.add(newFavorite)
              favoritesAdapter.notifyItemInserted(favoriteWeatherList.size - 1)
              saveFavorites()
              showToast("$cityName added to favorites!")
          }
      } else {
          showToast("$cityName is already in your favorites.")
      }
  }




    // Save favorite cities to SharedPreferences
    private fun saveFavorites() {
        val sharedPrefs = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val jsonString = Gson().toJson(favoriteWeatherList)
        editor.putString("favoriteWeatherList", jsonString)
        editor.apply()
    }

    // Display a short toast message
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
