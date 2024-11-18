package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.FavAdapter
import com.example.weatherapp.databinding.ActivityFavoritesBinding
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.launch
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment() {
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var weatherVM: WeatherVM
    private lateinit var favoritesAdapter: FavAdapter
    private val favoriteWeatherList = mutableListOf<ForCast>()
    private val HOUR_OF_DAY = "Hour_of_day"
    private val TIMME_PREFS = "time"

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Navigate to MainActivity
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish() // Close the current activity to clear stack
            }
        })
}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ActivityFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation", "CommitPrefEdits")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFavoriteCities()
/*
        saveFavorites()
*/

        val repository = WeatherRepository(RetrofitClient.apiService, requireActivity())
        val viewModelFactory = ViewModelFactory(repository)
        weatherVM = ViewModelProvider(requireActivity(), viewModelFactory)[WeatherVM::class.java]
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("locationResult")
            ?.observe(viewLifecycleOwner) { bundle ->
                val cityName = bundle.getString("cityName", "Unknown City")
                val latitude = bundle.getDouble("latitude", 0.0)
                val longitude = bundle.getDouble("longitude", 0.0)
                val Temp = bundle.getDouble("Temp", 0000.0000)
                val Time = bundle.getString("Time","00.00")
                val forecast = bundle.getParcelable<ForCast>("forecast")
                Log.d("FavoritesFragment", "Tempreature : $Temp")
                Log.d("FavoritesFragment", "CityName: $cityName")
                Log.d("FavoritesFragment","Time is : $Time")

                // Now, handle the retrieved data as needed

                if (forecast != null) {
                    updateFavoritesWithCity(forecast)
                }else
                    lifecycleScope.launch(Dispatchers.IO) {
                        binding.noFav.visibility = View.VISIBLE
                    }



            }


        // Floating Action Button for navigating to MapsFragment
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.mapsFragment)
        }

    }
    private fun setupRecyclerView() {
        favoritesAdapter = FavAdapter(
            favoriteWeatherList,
            { favoriteLocation -> GoToMainWithUpdatedLocation(favoriteLocation) },
            { favoriteLocation -> removeCityFromFavorites(favoriteLocation) }
        )
        binding.favoritesRV.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    private fun updateRecyclerViewVisibility() {
        if (favoriteWeatherList.isNullOrEmpty()) {
            binding.noFav.visibility = View.VISIBLE
            binding.favoritesRV.visibility = View.GONE
        } else {
            binding.noFav.visibility = View.GONE
            binding.favoritesRV.visibility = View.VISIBLE
            favoritesAdapter.notifyDataSetChanged()
        }
    }

    // Load favorite cities from Shared Preferences
    private fun loadFavoriteCities() {
        val sharedPreferences = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)

        val favoriteCitiesJson = sharedPreferences.getString("favoriteWeatherList", null)
        Log.i("FavoritesFragment", "Loaded favorites: $favoriteCitiesJson")


        if (!favoriteCitiesJson.isNullOrEmpty()) {
            try {
                val loadedCities = Gson().fromJson(favoriteCitiesJson, Array<ForCast>::class.java).toSet()
                favoriteWeatherList.clear()
                favoriteWeatherList.addAll(loadedCities)
                setupRecyclerView()
                updateRecyclerViewVisibility()
            } catch (e: Exception) {
                Log.e("SharedPreferences", "Error loading favorite cities: ${e.message}")
            }
        } else {
            binding.noFav.visibility = View.VISIBLE
            binding.favoritesRV.visibility = View.GONE
        }
    }



    private fun GoToMainWithUpdatedLocation(favoriteLocation: ForCast) {
       val Intent = Intent(requireContext(), MainActivity::class.java)
        Intent.putExtra("latitude", favoriteLocation.city.coord.lat)
        Intent.putExtra("longitude", favoriteLocation.city.coord.lon)
        Intent.putExtra(TIMME_PREFS,favoriteLocation.weatherList[0].dt_txt?.substring(11,16))
        saveTime(favoriteLocation.weatherList[0].dt_txt?.substring(11,16))

        startActivity(Intent)
    }

    private fun removeCityFromFavorites(favoriteLocation: ForCast) {
        lifecycleScope.launch(Dispatchers.IO) {
            SharedPrefs.getInstance(requireContext()).removeCity(favoriteLocation.city.name)
            favoriteWeatherList.remove(favoriteLocation)
            // Remove the city from the list
            saveFavorites()
            withContext(Dispatchers.Main) {
                showToast("${favoriteLocation.city.name} removed from favorites.")
                favoritesAdapter.notifyDataSetChanged()


                val iterator = favoriteWeatherList.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item?.city?.name == favoriteLocation.city.name &&
                        item.city.coord.lat == favoriteLocation.city.coord.lat &&
                        item.city.coord.lon == favoriteLocation.city.coord.lon
                    ) {
                        iterator.remove()

                        // Exit the loop after removing the item
                    }
                }

                if (favoriteWeatherList.isEmpty()) {
                    binding.noFav.visibility = View.VISIBLE // Show "No favorites" message
                    binding.favoritesRV.visibility = View.GONE // Hide RecyclerView
                } else {
                    binding.noFav.visibility = View.GONE // Hide "No favorites" message
                    binding.favoritesRV.visibility = View.VISIBLE // Show RecyclerView

                }
            }

        }
    }

    private fun updateFavoritesWithCity(forcast: ForCast) {
            favoriteWeatherList.add(forcast)
            setupRecyclerView()
        favoritesAdapter.notifyItemInserted(favoriteWeatherList.size - 1)
            saveFavorites()
            Log.i("cities :", "updateFavoritesWithCity: $favoriteWeatherList")
            showToast("${forcast.city.name} added to favorites!")


  }

    // Save favorite cities to SharedPreferences
    private fun saveFavorites() {
        val sharedPrefs = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val jsonString = Gson().toJson(favoriteWeatherList)
        editor.putString("favoriteWeatherList", jsonString)
        editor.apply()
    }
    private fun saveTime(Time:String?) {
        val sharedPreferences = requireContext().getSharedPreferences(TIMME_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(HOUR_OF_DAY,Time).apply()

    }
    // Display a short toast message
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
