package com.example.weatherapp.View

import SharedPrefs
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.NetworkUtils
import com.example.weatherapp.R
import com.example.weatherapp.adapter.FavAdapter
import com.example.weatherapp.databinding.ActivityFavoritesBinding
import com.example.weatherapp.databinding.CustomdialogBinding
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.weather_VM.ViewModelFactory
import com.example.weatherapp.weather_VM.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var sharedPrefs: SharedPrefs

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        sharedPrefs = SharedPrefs.getInstance(requireContext())
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ActivityFavoritesBinding.inflate(inflater, container, false)
        loadFavoriteCities()
        val repository = WeatherRepository(RetrofitClient.apiService, requireActivity())
        val viewModelFactory = ViewModelFactory(repository, sharedPrefs)
        weatherVM =
            ViewModelProvider(requireActivity(), viewModelFactory)[WeatherVM::class.java]

        arguments?.let { bundle ->
            val cityName = bundle.getString("cityName", "Unknown City")
            val latitude = bundle.getDouble("latitude", 0.0)
            val longitude = bundle.getDouble("longitude", 0.0)
            val Temp = bundle.getDouble("Temp", 0000.0000)
            val Time = bundle.getString("Time", "00.00")
            val forecast = bundle.getParcelable<ForCast>("forecast")
            Log.d("FavoritesFragment", "Tempreature : $Temp")
            Log.d("FavoritesFragment", "CityName: $cityName")
            Log.d("FavoritesFragment", "fullLocation: $cityName")
            Log.d("FavoritesFragment", "Time is : $Time")

            // Now, handle the retrieved data as needed
            if (forecast != null) {
                Log.i("FromFavoritesFragment", " Data Fetched")
                lifecycleScope.launch {
                    // Step 1: Update location in IO thread
                    val updateJob = launch(Dispatchers.Main) {
                        updateFavoritesWithCity(forecast)
                    }

                    updateJob.join() // Wait for the update to finish
                    // Step 2: Show RecyclerView on Main thread
                    withContext(Dispatchers.Main) {
                        binding.fragmentContainerView.visibility = View.GONE
                        binding.lottieAnimationView.visibility = View.GONE
                        binding.noFav.visibility = View.GONE
                    }
                }
            }
        }

        return binding.root

    }



            @SuppressLint("SuspiciousIndentation", "CommitPrefEdits")
            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                binding.lottieAnimationView.visibility = View.GONE



        // Floating Action Button for navigating to MapsFragment
        binding.fab.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(requireActivity())) {
                findNavController().navigate(R.id.mapsFragment)
            } else {
                showNoInternetSnackbar("You're out of internet connection cannot navigate to maps ")
            }


        }

    }

    private fun setupRecyclerView() {
        sharedPreferences = requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        favoritesAdapter = FavAdapter(
            favoriteWeatherList,
            requireContext(),
            { favoriteLocation -> GoToMainWithUpdatedLocation(favoriteLocation) },
            { favoriteLocation -> removeCityFromFavorites(favoriteLocation) },
        )

        binding.favoritesRV.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = favoritesAdapter

            val itemTouchHelperCallback =
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        return false // Drag-and-drop not supported
                    }
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val mainCard = viewHolder.itemView.findViewById<View>(R.id.favItem)
                        if (direction == ItemTouchHelper.LEFT) {
                            viewHolder.itemView.animate()
                                .translationX(0f)
                                .setDuration(0)
                                .withEndAction {
                                    // After resetting, show the dialog
                                }.start()

                            setCustomizedDialog(binding.root,viewHolder)

                        }
                    }
                    override fun onChildDraw(
                        c: Canvas,
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        dX: Float,
                        dY: Float,
                        actionState: Int,
                        isCurrentlyActive: Boolean
                    ) {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                    }

                }
            // Attach the ItemTouchHelper to the RecyclerView
            val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }
    private fun updateRecyclerViewVisibility() {
        if (favoriteWeatherList.isNullOrEmpty()) {
            binding.noFav.visibility = View.VISIBLE
            binding.favoritesRV.visibility = View.GONE
        } else {
            binding.noFav.visibility = View.GONE
            binding.favoritesRV.visibility = View.VISIBLE
        }
    }
    private fun showNoInternetSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // Load favorite cities from Shared Preferences
    private fun loadFavoriteCities() {
        val sharedPreferences =
            requireContext().getSharedPreferences("myprefs", Context.MODE_PRIVATE)

        val favoriteCitiesJson = sharedPreferences.getString("favoriteWeatherList", null)
        Log.i("FavoritesFragment", "Loaded favorites: $favoriteCitiesJson")


        if (!favoriteCitiesJson.isNullOrEmpty()) {
            try {
                val loadedCities =
                    Gson().fromJson(favoriteCitiesJson, Array<ForCast>::class.java).toSet()
                favoriteWeatherList.clear()
                favoriteWeatherList.addAll(loadedCities)
                setupRecyclerView()
                updateRecyclerViewVisibility()
            } catch (e: Exception) {
                Log.e("SharedPreferences", "Error loading favorite cities: ${e.message}")
            }
        } else {
            updateRecyclerViewVisibility()
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

    @SuppressLint("NotifyDataSetChanged")
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
                    if (item.city.name == favoriteLocation.city.name &&
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
        lifecycleScope.launch(Dispatchers.Main) {
            favoriteWeatherList.add(forcast)
            setupRecyclerView()
            favoritesAdapter.notifyItemInserted(favoriteWeatherList.size - 1)
            saveFavorites()

            // Ensure RecyclerView visibility is updated
            updateRecyclerViewVisibility()

            Log.i("cities :", "updateFavoritesWithCity: $favoriteWeatherList")
            showToast("${forcast.city.name} added to favorites!")
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
    private fun saveTime(Time:String?) {
        val sharedPreferences = requireContext().getSharedPreferences(TIMME_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(HOUR_OF_DAY,Time).apply()

    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun setCustomizedDialog(parent: ViewGroup, viewHolder: RecyclerView.ViewHolder){
        val position = viewHolder.adapterPosition

        val dialogInflater = getLayoutInflater()
        val dialogView = CustomdialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView.root)
            .setCancelable(true) // Allow dismissing the dialog by touching outside
            .create()
        dialogView.yesButton.setOnClickListener{
            val favoriteLocation = favoriteWeatherList[position]
            removeCityFromFavorites(favoriteLocation)
            favoritesAdapter.notifyItemRemoved(position)
            dialog.dismiss()
        }
        dialogView.cancelButton.setOnClickListener{
            // Reset swipe state
            favoritesAdapter.notifyItemChanged(position)
            dialog.dismiss()
        }
        dialog.show()
    }
}
