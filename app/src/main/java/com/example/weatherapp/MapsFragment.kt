package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import org.osmdroid.config.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.databinding.FragmentMapsBinding
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.location.Geocoder
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.SuggestionAdapter
import com.example.weatherapp.model.FavoriteLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import java.io.IOException
import java.util.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.MapEventsOverlay

class MapsFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var binding: FragmentMapsBinding
    private lateinit var fac: ViewModelFactory
    private lateinit var weatherVM: WeatherVM
    private lateinit var marker: Marker
    private var sourceActivity: String? = null
    private var selectedGeoPoint: GeoPoint? = null
    private val suggestionList = mutableListOf<String>()
    private lateinit var suggestionAdapter: SuggestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        fac = ViewModelFactory(WeatherRepository(RetrofitClient.apiService, requireContext()))
        weatherVM = ViewModelProvider(this, fac).get(WeatherVM::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun initializeMap() {
        mapView = binding.map
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)

        mapView.controller.setZoom(5.0)
/*
        mapView.controller.setCenter(GeoPoint(51.505, -0.09))
*/
        mapView.invalidate()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupMarker() {
        marker = Marker(mapView)


        marker.icon = resources.getDrawable(R.drawable.placeholder)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                marker.position = GeoPoint(marker.position.latitude, marker.position.longitude)
                return true
            }
        })
        marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {

            override fun onMarkerDrag(marker: Marker) {
                // Do nothing while dragging
            }

            override fun onMarkerDragEnd(marker: Marker) {

                updateLocation(marker.position) // Call for favorites
            }

            override fun onMarkerDragStart(marker: Marker) {
                // Do nothing on drag start
            }
        })

        marker.isDraggable = true
    }

    private fun updateLocation(newLocation: GeoPoint) {
        lifecycleScope.launch {
            // Do something with cityName if needed
            weatherVM.forecast.observe(viewLifecycleOwner) { forecast ->
                // Display forecast or handle it in the UI
                Toast.makeText(requireContext(), "Forecast: $forecast", Toast.LENGTH_SHORT).show()
                val cityName = forecast.city.name
                Log.i("From MapsFragment", "City Name: $cityName")
                val result = Bundle().apply {
                    putString("cityName", cityName)
                    putDouble("latitude", newLocation.latitude)
                    putDouble("longitude", newLocation.longitude)
                }
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "locationResult",
                    result
                )
            }


        }
    }

    private suspend fun onLocationSelected(latitude: Double, longitude: Double): String {
        // Fetch the city name based on updated coordinates
        val cityName = getCityNameFromCoordinates(latitude, longitude)

        // Send the location and city name to FavoritesFragment
        val result = Bundle().apply {
            putString("cityName", cityName)
            putDouble("latitude", latitude)
            putDouble("longitude", longitude)
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set("locationResult", result)

        return cityName
    }

    private fun updateForFavoritesLocation(newLocation: GeoPoint) {
        lifecycleScope.launch(Dispatchers.IO) {

            // Send the location data back to MainActivity
            val intent = Intent(activity, FavoritesFragment::class.java)
            intent.putExtra("latitude", newLocation.latitude)
            intent.putExtra("longitude", newLocation.longitude)
            activity?.setResult(AppCompatActivity.RESULT_OK, intent)
            startActivity(intent)           // Finish the fragment after location update
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
        initializeMap()
        setupMarker()
        setupMapClickListener()

        setupSearchFunctionality()

    }

    private fun setupMapClickListener() {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(geoPoint: GeoPoint?): Boolean {
                if (geoPoint != null) {
                    // Handle the click event here
                    onMapClicked(geoPoint)
                    updateMarkerPosition(geoPoint)
                }
                return true
            }

            override fun longPressHelper(geoPoint: GeoPoint?): Boolean {
                // Handle long press if needed
                return false
            }
        }

        // Add the event overlay to capture map taps
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)
    }

    private fun onMapClicked(geoPoint: GeoPoint) {
        // Do something with the clicked location (geoPoint)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        // Set title or message if needed
        builder.setTitle("Location Options")
        builder.setMessage("Choose an option for this location")

        // Update location button
        builder.setPositiveButton("Update Location") { dialog, id ->
            // Handle the update location action
            updateLocation(geoPoint)
            Toast.makeText(requireContext(), "Location Updated", Toast.LENGTH_SHORT).show()
        }

        // Just navigate button
        builder.setNegativeButton("Just Navigate") { dialog, id ->
            // Handle the just navigate action
            navigateToLocation(geoPoint)
            Toast.makeText(requireContext(), "Navigating to location", Toast.LENGTH_SHORT).show()
        }
        // Cancel button
        builder.setNeutralButton("Home") { dialog, id ->
            GoToMainWithUpdatedLocation(geoPoint)
        }

        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.show() // Show the dialog

        // Optional: show a toast with the clicked coordinates
        Toast.makeText(
            requireContext(),
            "Clicked at: ${geoPoint.latitude}, ${geoPoint.longitude}",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun GoToMainWithUpdatedLocation(geoPoint: GeoPoint) {
        val Intent = Intent(requireContext(), MainActivity::class.java)
        Intent.putExtra("latitude", geoPoint.latitude)
        Intent.putExtra("longitude", geoPoint.longitude)
        startActivity(Intent)
    }
    private suspend fun getCityNameFromCoordinates(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses =
                withContext(Dispatchers.IO) { geocoder.getFromLocation(latitude, longitude, 1) }
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].locality ?: "Unknown City"
            } else {
                "Unknown City"
            }
        } catch (e: Exception) {
            Log.e("FavoritesFragment", "Error geocoding: ${e.message}", e)
            "Unknown City"
        }
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses =
                    geocoder.getFromLocationName(location, 10) // Get up to 10 suggestions

                if (addresses != null && addresses.isNotEmpty()) {
                    // Find the address that matches the selected suggestion
                    val selectedAddress = addresses.find { address ->
                        "${address.locality ?: address.subAdminArea ?: address.adminArea}, ${address.countryName}" == location
                    }

                    selectedAddress?.let { address ->
                        selectedGeoPoint = GeoPoint(address.latitude, address.longitude)
                        Log.i("From MapsFragment", "Selected GeoPoint: $selectedGeoPoint")
                          navigateToLocation(selectedGeoPoint!!)
                        withContext(Dispatchers.Main) {
                            binding.suggestionsRecyclerView.visibility =
                                View.GONE // Hide suggestions
                            navigateToLocation(selectedGeoPoint!!)
                            updateMarkerPosition(selectedGeoPoint!!)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    // Search for a location based on the user's query

    @SuppressLint("SuspiciousIndentation")
    private fun setupSearchFunctionality() {
        suggestionAdapter = SuggestionAdapter(suggestionList) { selectedSuggestion ->
            searchLocation(selectedSuggestion)
        }
        binding.suggestionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.suggestionsRecyclerView.adapter = suggestionAdapter
            binding.locationSearch.setOnQueryTextListener(
            object :
                android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!query.isNullOrEmpty()) {
                        fetchSuggestions(query)
                        binding.suggestionsRecyclerView.visibility = View.VISIBLE

                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (!newText.isNullOrEmpty()) {
                            fetchSuggestions(newText)
                        binding.suggestionsRecyclerView.visibility = View.VISIBLE
                    }else {
                        showSuggestions(suggestionList)
                        binding.suggestionsRecyclerView.visibility = View.GONE
                    }
                    return true
                }
            })


    }

    private fun fetchSuggestions(query: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocationName(query, 10) // Get up to 10 suggestions
                suggestionList.clear()

                // Add city and country information to suggestionList
                if (addresses != null) {
                    for (address in addresses) {
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea
                        val country = address.countryName

                        if (city != null && country != null) {
                            suggestionList.add("$city, $country")
                        } else if (city != null) {
                            suggestionList.add(city)
                        }
                        else if (country != null) {
                            suggestionList.add(country)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    suggestionAdapter.updateData(suggestionList)
                    suggestionAdapter.notifyDataSetChanged()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error finding location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSuggestions(suggestions: List<String>) {
        lifecycleScope.launch(Dispatchers.Main) {
            suggestionAdapter.updateData(suggestions) // Update the adapter with new suggestions
            suggestionAdapter.notifyDataSetChanged()
        }
    }

    // Update marker position when user searches for a location
    private fun updateMarkerPosition(geoPoint: GeoPoint) {
        if (::marker.isInitialized) {
            marker.position = geoPoint
            binding.map.controller.setCenter(geoPoint) // Center the map on the new marker position
            binding.map.controller.setZoom(10.0) // Zoom into the location

        } else {
            // Initialize the marker if not already set
            marker = Marker(mapView)
            marker.position = geoPoint
            binding.map.overlays.add(marker)
        }
        binding.map.invalidate()
        fetchWeatherByLocation(geoPoint.latitude, geoPoint.longitude)
        Log.i("From MapsFragment", "Marker position updated: $geoPoint")
    }
    private fun navigateToLocation(geoPoint: GeoPoint){
        binding.map.controller.setCenter(geoPoint)
        binding.map.controller.setZoom(10.0)
    }


    private fun fetchWeatherByLocation(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                weatherVM.updateWeatherFromMap(latitude, longitude)

            } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching weather data from MAPSFRAGMENT",
                        Toast.LENGTH_SHORT
                    ).show()

            }
        }
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume() // Important for resuming map view
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // Important for pausing map view
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.Main) {
            mapView.onDetach() // Clean up map view
        }
    }
}

