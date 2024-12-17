package com.example.weatherapp.View

import SharedPrefs
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import org.osmdroid.config.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.databinding.FragmentMapsBinding
import com.example.weatherapp.weather_VM.ViewModelFactory
import com.example.weatherapp.weather_VM.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils.navigateUpTo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.R
import com.example.weatherapp.utils.ToastUtil
import com.example.weatherapp.adapter.SuggestionAdapter

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import java.io.IOException
import java.util.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay

class MapsFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var binding: FragmentMapsBinding
    private lateinit var fac: ViewModelFactory
    private lateinit var weatherVM: WeatherVM
    private lateinit var marker: Marker

    private val suggestionList = mutableListOf<String>()
    private lateinit var suggestionAdapter: SuggestionAdapter
    private lateinit var sharedPreferences:SharedPrefs
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = SharedPrefs(requireContext())
        Configuration.getInstance().userAgentValue = requireContext().packageName
        fac = ViewModelFactory(
            WeatherRepository(RetrofitClient.apiService, requireContext()),
            sharedPreferences
        )
        weatherVM = ViewModelProvider(this, fac).get(WeatherVM::class.java)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .remove(this@MapsFragment)
                        .commit()
                    findNavController().navigateUp()
                   /* val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.Home, true)
                        .build()
                    findNavController().navigate(R.id.Home,navOptions)*/
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        initializeMap()

        return binding.root
    }
    fun onBackPressedFromView(view: View) {
        val intent =Intent(requireContext(), MainActivity::class.java)
        navigateUpTo(requireActivity(),intent)
        findNavController().navigate(R.id.Home)
    }


    private fun initializeMap() {
        mapView = binding.map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(5.0)
        mapView.invalidate()
        val overlay = object : Overlay() {
            override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
                super.draw(canvas, mapView, shadow)

                if (canvas == null || mapView == null) return

                val cities = listOf(
                    "London" to GeoPoint(51.5074, -0.1278),
                    "Paris" to GeoPoint(48.8566, 2.3522),
                    "Berlin" to GeoPoint(52.52, 13.4050),
                    "New York" to GeoPoint(40.7128, -74.0060),
                    "Tokyo" to GeoPoint(35.6895, 139.6917),
                    "Sydney" to GeoPoint(-33.8651, 151.2099),
                    "Rome" to GeoPoint(41.9028, 12.4964),

                    // Add other cities as needed
                )

                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 30f // Set the desired font size
                    isAntiAlias = true
                }

                for ((cityName, location) in cities) {
                    val screenPoint = Point()
                    mapView.projection.toPixels(location, screenPoint)
                    canvas.drawText(cityName, screenPoint.x.toFloat(), screenPoint.y.toFloat(), paint)
                }
            }
        }
        mapView.overlays.add(overlay)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupMarker() {
        marker = Marker(mapView)
        marker.icon = resources.getDrawable(R.drawable.gps)
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
                marker.position = GeoPoint(marker.position.latitude,marker.position.longitude)
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
            // Do something with cityName if needed
            weatherVM.forecast.observe(viewLifecycleOwner) { forecast ->
                // Display forecast or handle it in the UI
             Log.i("From MapsFragment", "Forecast: $forecast")
                val cityName = forecast?.city?.name
                val Temp = forecast!!.weatherList[0].main.temp
                val Time = forecast.weatherList[0].dt_txt!!.subSequence(11, 16).toString()
                Log.i("From MapsFragment", "City Name: $cityName")
                Log.i("From MapsFragment", "time: $Time")
                val result = Bundle().apply {
                    putString("cityName", cityName)
                    putDouble("latitude", newLocation.latitude)
                    putDouble("longitude", newLocation.longitude)
                    putDouble("Temp", Temp)
                    putString("Time", Time)
                    putParcelable("forecast", forecast)
                }
                findNavController().navigate(R.id.favorites, result)

            }
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
                    onBackPressed()
                }
            })


        mapView.minZoomLevel = 10.0
        mapView.maxZoomLevel = 15.0
        setupMarker()
        setupMapClickListener()

        setupSearchFunctionality()

    }
    fun onBackPressed() {
        // Find the FragmentContainerView by its ID
        findNavController().navigate(R.id.Home)

    }
    private fun setupMapClickListener() {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(geoPoint: GeoPoint?): Boolean {
                if (geoPoint != null) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    onMapClicked(geoPoint)
                    updateMarkerPosition(geoPoint,10.0)
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

    @SuppressLint("ResourceType")
    private fun onMapClicked(geoPoint: GeoPoint) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        // Set title or message if neede
        builder.setTitle(getString(R.string.Location_Dialog_Title))
        builder.setMessage(getString(R.string.Location_Dialog_Message))

        // Update location button
        builder.setPositiveButton(getString(R.string.Location_Dialog_update)) { _, _ ->
            // Handle the update location action
                updateLocation(geoPoint)

            ToastUtil.showCustomToast(requireContext(), "Location Updated ")
        }

        // Just navigate button
        builder.setNeutralButton (getString(R.string.Location_Dialog_Navigate)) { dialog, id ->
            // Handle the just navigate action
            navigateToLocation(geoPoint,10.0)
            ToastUtil.showCustomToast(requireContext(), "Navigating to location")

        }
        // Cancel button
        builder.setNeutralButton(getString(R.string.Location_Dialog_Home)) { dialog, id ->
            GoToMainWithUpdatedLocation(geoPoint)

        }
        builder.setNegativeButton(getString(R.string.Cancel), DialogInterface.OnClickListener { dialog, id ->

        })
        builder.setCancelable(true)

        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.secondary
            ))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.RED
            ))
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.secondary
            ))
        }
        dialog.setOnDismissListener(){
            dialog.dismiss()
        }
        dialog.show() // Show the dialog
        // Optional: show a toast with the clicked coordinates
        ToastUtil.showCustomToast(
            requireContext(),
            "Clicked at: ${geoPoint.latitude}, ${geoPoint.longitude}"
        )
    }
    fun showUpdateLocationDialog(
        context: Context,
        navController: NavController ,
        geoPoint: GeoPoint
    ) {
        val fragmentManager = requireActivity().supportFragmentManager
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        // Set title or message if neede
        builder.setTitle(getString(R.string.Location_Dialog_Title))
        builder.setMessage(getString(R.string.Location_Dialog_Message))

        // Update location button
        builder.setPositiveButton(getString(R.string.Location_Dialog_update)) { _, _ ->
            // Handle the update location action
            updateLocation(geoPoint)
            ToastUtil.showCustomToast(requireContext(), "Location Updated")

        }

        // Just navigate button
        builder.setNeutralButton (getString(R.string.Location_Dialog_Navigate)) { dialog, id ->
            // Handle the just navigate action
            navigateToLocation(geoPoint,10.0)
            ToastUtil.showCustomToast(requireContext(), "Navigating to location")

        }
        // Cancel button
        builder.setNeutralButton(getString(R.string.Location_Dialog_Home)) { dialog, id ->
            GoToMainWithUpdatedLocation(geoPoint)

        }
        builder.setNegativeButton(getString(R.string.Cancel), DialogInterface.OnClickListener { dialog, id ->

        })
        builder.setCancelable(true)

        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.secondary
            ))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.RED
            ))
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.secondary
            ))
        }
        dialog.setOnDismissListener(){
            dialog.dismiss()
        }
        dialog.show() // Show the dialog
        // Optional: show a toast with the clicked coordinates
        ToastUtil.showCustomToast(
            requireContext(),
            "Clicked at: ${geoPoint.latitude}, ${geoPoint.longitude}"
        )
    }
    fun navigateToFavoritesFragment(fragmentManager: FragmentManager) {
        lifecycleScope.launch(Dispatchers.Main) {
            val favoritesFragment = FavoritesFragment()
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, favoritesFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun GoToMainWithUpdatedLocation(geoPoint: GeoPoint) {
        ToastUtil.showCustomToast(requireActivity(), "Navigating to Home")
        val Intent = Intent(requireContext(), MainActivity::class.java)
        Intent.putExtra("latitude", geoPoint.latitude)
        Intent.putExtra("longitude", geoPoint.longitude)
        startActivity(Intent)
    }

    @SuppressLint("SuspiciousIndentation")
    private  fun searchLocation(location: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

            Log.i("From MapsFragment", "Searching for location: $location")
            val addresses = geocoder.getFromLocationName(location, 30) // Get up to 10 suggestions
            Log.i("From MapsFragment", "Addresses found: $addresses")

            // Improved matching logic
            val selectedAddress = addresses!!.find { address ->
                address.locality?.equals(location, ignoreCase = true) == true ||
                        address.subAdminArea?.equals(location, ignoreCase = true) == true ||
                        address.adminArea?.equals(location, ignoreCase = true) == true ||
                        address.countryName?.equals(location, ignoreCase = true) == true

            }
            navigateToLocation(
                GeoPoint(addresses.first()!!.latitude, addresses.first().longitude),
                18.0
            )
        showUpdateLocationDialog(requireContext(),findNavController(), GeoPoint(addresses.first()!!.latitude, addresses.first().longitude))
            Log.i("From MapsFragment", "Selected Address: $selectedAddress")

            selectedAddress?.apply {
                val geoPoint = GeoPoint(this.latitude, this.longitude)
                Log.i(
                    "From MapsFragment",
                    "GeoPoint: Latitude: ${geoPoint.latitude}, Longitude: ${geoPoint.longitude}"
                )
                navigateToLocation(GeoPoint(this.latitude, this.longitude), 15.0)
                // Return the GeoPoint for successful matches
            } ?: run {
                Log.i("From MapsFragment", "No matching address found for: $location")
            }
        }
    private fun updateCityName(fullLocation: String) {
        val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("fullLocation", fullLocation)
        editor.apply()
    }

    // Initialize the search functionality
    private fun setupSearchFunctionality() {
        suggestionAdapter = SuggestionAdapter(suggestionList) { selectedSuggestion ->
                searchLocation(selectedSuggestion)
                updateCityName(selectedSuggestion)
        }

        binding.suggestionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.suggestionsRecyclerView.adapter = suggestionAdapter


        // Search view listener for input handling
        binding.locationSearch.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    fetchSuggestions(query)
                }else{
                    binding.locationSearch.clearFocus()
                    binding.suggestionsRecyclerView.visibility=View.GONE

                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    fetchSuggestions(newText)
                }else{
                    binding.locationSearch.clearFocus()
                    binding.suggestionsRecyclerView.visibility=View.GONE

                }
                return true
            }
        })

    }

    // Fetch suggestions based on the user's input
    private fun fetchSuggestions(query: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch up to 10 addresses for the query
                val addresses = geocoder.getFromLocationName(query, 10)

                val suggestions = addresses?.map { address ->
                    val addressLines = mutableListOf<String>()

                    // Add each address line (if present)
                    for (i in 0..address.maxAddressLineIndex) {
                        address.getAddressLine(i)?.let { addressLines.add(it) }
                    }

                    // Add additional details (if available)
                    address.locality?.let { addressLines.add(it) } // City // Sub-admin area
                    address.adminArea?.let { addressLines.add(it) } // Admin area
                    address.countryName?.let { addressLines.add(it) } // Country

                    // Combine all parts into a single, comma-separated string
                    addressLines.joinToString(", ")
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    binding.suggestionsRecyclerView.visibility =View.VISIBLE
                    suggestionList.clear()
                    suggestionList.addAll(suggestions)
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


    // Update marker position when user searches for a location
    private fun updateMarkerPosition(geoPoint: GeoPoint,zoomlevel: Double) {
        if (::marker.isInitialized) {
            marker.position = geoPoint
            binding.map.controller.setCenter(geoPoint) // Center the map on the new marker position
            binding.map.controller.setZoom(zoomlevel) // Zoom into the location

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
    private fun navigateToLocation(geoPoint: GeoPoint,zoomlevel:Double){
        binding.map.controller.setCenter(geoPoint)
        binding.map.controller.setZoom(zoomlevel)

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
        lifecycleScope.launch {
            mapView.onDetach()
        }
    }



}

