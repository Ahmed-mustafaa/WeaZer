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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.SuggestionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import java.io.IOException
import java.util.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.MapEventsOverlay

class FavoritesMap : Fragment() {
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
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(51.505, -0.09))
        mapView.invalidate()
    }
    private fun setupMarker() {
        marker = Marker(mapView)
        marker.icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation)
        marker.position = GeoPoint(51.505, -0.09)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        marker.setOnMarkerClickListener(object: Marker.OnMarkerClickListener {
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

                updateForFavoritesLocation(marker.position) // Call for favorites
            }

            override fun onMarkerDragStart(marker: Marker) {
                // Do nothing on drag start
            }
        })

        marker.isDraggable = true
    }

    private fun updateForFavoritesLocation(newLocation: GeoPoint) {
        val bundle = Bundle().apply {
            putDouble("latitude", newLocation.latitude)
            putDouble("longitude", newLocation.longitude)
        }
        parentFragmentManager.setFragmentResult("locationRequestKey", bundle)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sourceActivity = activity?.intent?.getStringExtra("source")

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
            updateForFavoritesLocation(geoPoint)
            Toast.makeText(requireContext(), "Location Updated", Toast.LENGTH_SHORT).show()
        }

        // Just navigate button
        builder.setNegativeButton("Just Navigate") { dialog, id ->
            // Handle the just navigate action
            navigateToLocation(geoPoint)
            Toast.makeText(requireContext(), "Navigating to location", Toast.LENGTH_SHORT).show()
        }

        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.show() // Show the dialog

        // Optional: show a toast with the clicked coordinates
        Toast.makeText(requireContext(), "Clicked at: ${geoPoint.latitude}, ${geoPoint.longitude}", Toast.LENGTH_SHORT).show()
    }



    // Search for a location based on the user's query
    private fun searchLocation(location: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO){
            try {
                val addresses = geocoder.getFromLocationName(location, 10)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    for (i in addresses.indices) {
                        val address = addresses[i]
                        selectedGeoPoint = GeoPoint(address.latitude, address.longitude)
                        Log.i("From MapsFragment", "Selected GeoPoint: $selectedGeoPoint")
                        /*
                                                selectedGeoPoint = GeoPoint(address.latitude, address.longitude)
                        */
                    }
                    withContext(Dispatchers.Main) {
                        suggestionAdapter.updateData(suggestionList)
                        showSuggestions(suggestionList)
                        navigateToLocation(selectedGeoPoint!!)
                        updateMarkerPosition(selectedGeoPoint!!)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error finding location", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

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
                        /*
                                                showSuggestions(suggestionList)
                        */
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
                val addresses = geocoder.getFromLocationName(query, 10)
                suggestionList.clear()
                addresses?.forEach { address ->
                    suggestionList.add("${address.locality}, ${address.countryName}")
                }

                // Ensure UI updates happen on the main thread
                withContext(Dispatchers.Main) {
                    showSuggestions(suggestionList)  // This will update the adapter
                }
            } catch (e: IOException) {
                e.printStackTrace()

                // Show error message on the main thread
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
            binding.map.controller.setZoom(15.0) // Zoom into the location
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
        binding.map.controller.setZoom(15.0)
    }


    fun fetchWeatherByLocation(latitude: Double, longitude: Double) {
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

