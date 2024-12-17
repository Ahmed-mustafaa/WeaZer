package com.example.weatherapp.View

import SharedPrefs
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import android.Manifest
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.net.ConnectivityManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.AlarmReceiver
import com.example.weatherapp.NetworkStateReceiver
import com.example.weatherapp.NetworkUtils
import com.example.weatherapp.R
import com.example.weatherapp.utils.ToastUtil
import com.example.weatherapp.adapter.DailyAdapter
import com.example.weatherapp.adapter.WeatherTodayAdapter
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.model.WeatherList
import com.example.weatherapp.weather_VM.ViewModelFactory
import com.example.weatherapp.weather_VM.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherVM: WeatherVM
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private var isInitialLaunch = true
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private val PREFS_NAME = "weather_prefs"
    private val TEMP_UNIT_KEY = "temp_unit"
    private val WIND_UNIT_KEY = "wind_unit"
    private var HourFromFragment = "222"
    private var selectedUnit: String = "Celsius"
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    private val LOCATION_REQUEST_CODE = 1001
    private var cityNameFromFragment: String? = null
    private val ALarm_PREFS_NAME = "alarmPrefs"
    private val ALARM_SET_KEY = "alarmSet"

    private var isArabic = Locale.getDefault().language == "ar"

    private lateinit var sharedPrefsHelper: SharedPrefs
    private lateinit var networkStateReceiver: NetworkStateReceiver
    private var isConnected = true
    private var connectionDialog: AlertDialog? = null


    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lottieAnimationView.visibility = View.VISIBLE

        drawerLayout = binding.drawer
        val toolbar = binding.toolbar.apply {
            binding.back.visibility = View.GONE
        }
        binding.toolbar.title = getString(R.string.Home)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        toggleFullScreen(true)
        supportActionBar?.hide()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        setSupportActionBar(toolbar)

        // Initialize NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configure drawer toggle
        val drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        val navigationView : NavigationView = binding.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Home_title -> {
                    if(!NetworkUtils.isNetworkAvailable(this))
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    else {
                        binding.toolbar.title = getString(R.string.Home)
                        binding.mainLayout.visibility = View.VISIBLE
                        binding.navHostFragment.visibility = View.GONE
                        binding.navigationView.visibility = View.GONE
                        navController.popBackStack()
                        drawerLayout.closeDrawer(GravityCompat.START)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                    true
                }

                R.id.Alarm_title -> {
                    // Navigate to Alarm Fragment
                    if(!NetworkUtils.isNetworkAvailable(this))
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    else {
                        binding.toolbar.title = getString(R.string.Alarms)
                        binding.back.visibility = View.VISIBLE
                        binding.mainLayout.visibility = View.GONE
                        binding.navHostFragment.visibility = View.VISIBLE
                        binding.navigationView.visibility = View.VISIBLE
                        navController.navigate(R.id.Alarm)
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }

                R.id.Favorites_title -> {
                    // Navigate to Favorites
                    if(!NetworkUtils.isNetworkAvailable(this))
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    else {
                        binding.toolbar.title = getString(R.string.Favorites)
                        binding.back.visibility = View.VISIBLE
                        binding.mainLayout.visibility = View.GONE
                        binding.navHostFragment.visibility = View.VISIBLE
                        binding.navigationView.visibility = View.VISIBLE
                        navController.navigate(
                            R.id.favorites, null, NavOptions.Builder()
                                .setPopUpTo(
                                    R.id.drawer,
                                    inclusive = false
                                ) // Clear back stack up to mainScreen
                                .build()
                        )
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }

                R.id.Settings_title -> {
                    if(!NetworkUtils.isNetworkAvailable(this))
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    else {
                        binding.toolbar.title = getString(R.string.Settings)
                        binding.back.visibility = View.VISIBLE
                        binding.mainLayout.visibility = View.GONE
                        binding.navHostFragment.visibility = View.VISIBLE
                        binding.navigationView.visibility = View.VISIBLE
                        navController.navigate(R.id.settings)
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true

                }

                else -> false
            }
        }
        setupViewModel()

        if(!NetworkUtils.isNetworkAvailable(this)){
            showConnectToInternetDialog()
            showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
        }
        else{
           handleInitialLocation()
        }


        networkStateReceiver = NetworkStateReceiver { isConnected ->
            if (isInitialLaunch) {
                // Skip Snackbar during the initial launch but still fetch data
                if (isConnected) {
                    handleInitialLocation()
                    observeWeatherData()
                }
                isInitialLaunch = false
                return@NetworkStateReceiver
            }

            if (isConnected) {

                // Show Snackbar and fetch data when connection is restored after a change
              lifecycleScope.launch {
                  showNoInternetLayout(false)
                  binding.swipeRefreshLayout.isRefreshing = true
                  delay(1500)
                  showSnackbar(getString(R.string.connectionRestored))
                  binding.swipeRefreshLayout.isRefreshing = false
                  connectionDialog?.dismiss()
                  handleInitialLocation()
                  observeWeatherData()
              }
            } else {
                // Handle no connection scenario
                lifecycleScope.launch {
                    connectionDialog = showConnectToInternetDialog()
                    if (connectionDialog!!.isShowing) {
                        connectionDialog?.setOnDismissListener {
                            lifecycleScope.launch {
                                binding.CahcedText.visibility = View.VISIBLE
                                    delay(3000)
                                currentLat = getSharedPreferences("myprefs", MODE_PRIVATE).getFloat("latitude", 0f).toDouble()
                                currentLon = getSharedPreferences("myprefs", MODE_PRIVATE).getFloat("longitude", 0f).toDouble()
                                fetchWeatherDataForLocation(currentLat!!, currentLon!!)
                                observeWeatherData()
                            }

                        }

                    }
                }
            }
        }
        setupAdapters(selectedUnit)
        requestNotificationPermission(this)
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isAlarmSet = sharedPreferences.getBoolean(ALARM_SET_KEY, false)

        // If the alarm hasn't been set, set it and mark it as set in SharedPreferences
        if (!isAlarmSet) {
            // Set the alarm only once
            GlobalScope.launch { // Using GlobalScope for the suspend function
                setupAlarm()

                // Mark the alarm as set in SharedPreferences
                with(sharedPreferences.edit()) {
                    putBoolean(ALARM_SET_KEY, true)
                    apply()
                }
            }
        }
        ToastUtil.requestBatteryOptimizationPermission(this)


        binding.pickuplocation.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                navigateToMapFragment()
            } else {
                showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
            }
        }
        // 1. Trigger weather data refresh here
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Check network availabilit
            binding.swipeRefreshLayout.isRefreshing = true
            lifecycleScope.launch {
                if (NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                    getCurrentLocation { currentLat, currentLon ->
                        if (currentLat != null && currentLon != null) {
                           fetchWeatherDataForLocation(currentLat,currentLon)
                        } else {
                            Log.e("MainActivity", "Failed to retrieve location.")
                        }
                    }
                    } else {
                    lifecycleScope.launch {

                    val sharedPreferences = getSharedPreferences("myprefs", Context.MODE_PRIVATE)
                    val gson = Gson()

// Retrieve the JSON string
                    val forecastJson = sharedPreferences.getString("forecast", null)

// Parse the JSON back into its original type
                    val forecast: ForCast? = forecastJson?.let {
                        gson.fromJson(it, ForCast::class.java)
                    }
                    if (forecast != null) {
                        updateUI(forecast)
                    } else
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                }
                }


                }
                binding.swipeRefreshLayout.isRefreshing = false


            }
        }

    fun onBackPressedFromView(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        navigateUpTo(intent)
          navController.navigate(R.id.Home)
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )

        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            supportActionBar?.hide()
        }
    }
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }
private fun offlineContent(){

    val sharedPreferences = getSharedPreferences("myprefs", Context.MODE_PRIVATE)
    val gson = Gson()
    lifecycleScope.launch {
// Retrieve the JSON string
        val forecastJson = sharedPreferences.getString("forecast", null)

// Parse the JSON back into its original type
        val forecast: ForCast? = forecastJson?.let {
            gson.fromJson(it, ForCast::class.java)
        }
        if (forecast != null) {
            updateUI(forecast)
        } else
            showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
    }
}
    private fun handleNetworkChange(isConnected: Boolean) {
        if (isConnected) {
            showSnackbar(getString(R.string.connectionRestored))
            lifecycleScope.launch {
                binding.lottieAnimationView.visibility = View.VISIBLE
                handleInitialLocation()
                Log.i("HandleNetworkChange", "Getiting into handleNetworkChange")

            }
            // Take action when internet is restored (e.g., fetch fresh data)
        } else {
            showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }






    @SuppressLint("ScheduleExactAlarm")
    private   fun setupAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("ALarmFromMAin", "This means the SDK is Higher >  26  ")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val timeInMillis = System.currentTimeMillis() // 10 seconds from now
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("NOTIFICATION_TYPE", "AUTO") // Mark this as a manual alarm
            intent.putExtra("ALARM_TIME", timeInMillis)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Set the alarm to trigger in 1 minute for testing purposes
            val triggerTime = System.currentTimeMillis()
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("MainActivity", "Alarm set to trigger in 1 minute")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        }
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, you can now show notifications
                Log.d("MainActivity", "Notification permission granted")
            } else {
                // Permission denied, handle the case where permission was not granted
                Log.d("MainActivity", "Notification permission Denied")

            }
        }else{
            promptEnableLocationServices()
        }
    }


    private fun requestNotificationPermission(context: Context) {
        // Only request permission if on Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Register the permission request launcher
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        Log.d("MainActivity", "Notification permission granted")
                    } else {
                        Log.e("MainActivity", "Notification permission denied")
                    }
                }
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "Notification permission already granted")
            }
        }
    }


    private fun setupViewModel() {
        sharedPrefsHelper = SharedPrefs(this)
        val repository = WeatherRepository(RetrofitClient.apiService, this)
        val factory = ViewModelFactory(repository, sharedPrefsHelper)
        weatherVM = ViewModelProvider(this, factory)[WeatherVM::class.java]
    }

    private fun setupAdapters(selectedUnit: String) {
        val adapter = WeatherTodayAdapter(emptyList(), "Celsius").apply {
            this.selectedUnit = selectedUnit
            notifyDataSetChanged()
            updateUnit(selectedUnit)
        }
        val Dailyadapter = DailyAdapter(emptyList(), "Celsius").apply {
            this.selectedUnit = selectedUnit
            notifyDataSetChanged()
            updateUnit(selectedUnit)
        }

        binding.hourlyRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dailyRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.hourlyRec.adapter = adapter
        binding.dailyRec.setHasFixedSize(true)
        binding.dailyRec.adapter = Dailyadapter
    }


    private fun navigateToMapFragment() {
        binding.fragmentContainerView.visibility = View.VISIBLE
        /*binding.mainLayout.visibility = View.GONE*/
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, MapsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun handleInitialLocation() {
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val sharedPreferences = this.getSharedPreferences("myprefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
Log.i("MainActivity", "Received latitude: $latitude, longitude: $longitude")

        when {
            latitude != 0.0 && longitude != 0.0 -> {
                // If lat and lon are provided through intent, fetch weather for that location
                fetchWeatherDataForLocation(latitude, longitude)
                Log.i("A7a", "Location data available")
            }

            else-> {
                Log.i("MainActivity", "No location data available")
                if (checkedPermissions() && (latitude == 0.0 && longitude == 0.0))  {
                    Log.i("A7ten", "No location data available")
                // If permissions are granted, get current location
                getCurrentLocation { currentLat, currentLon ->
                    if (currentLat != null && currentLon != null) {
                          fetchWeatherDataForCurrentLocation()

                        editor.putFloat("latitude", currentLat.toFloat())
                        editor.putFloat("longitude", currentLon.toFloat())
                        editor.apply()
                        Log.i("MainActivity", "currentLat: $currentLat, currentLon: $currentLon")
                    } else {
                        Log.e("MainActivity", "Failed to retrieve location.")
                    }

                }
            }

            }
        }
    }
    private fun saveInitialLocation(latitude: Double, longitude: Double) {
        val sharedPreferences = this.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putFloat("latitude", latitude.toFloat())
            putFloat("longitude", longitude.toFloat())
            apply()
        }
    }
    private fun getCurrentLocation(onLocationRetrieved: (Double?, Double?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.i(
                    "MainActivity",
                    "Current Location -> Latitude: $latitude, Longitude: $longitude"
                )
                onLocationRetrieved(latitude, longitude)
            } else {
                Log.e("MainActivity", "Failed to get last known location.")
                onLocationRetrieved(null, null)
            }
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Error getting location: ${exception.message}")
            onLocationRetrieved(null, null)
        }
    }

    private fun showLocationEnableDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Required")
        builder.setMessage("Please enable location services to use this app.")
        builder.setPositiveButton("OK") { _, _ ->
            // Open location settings
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // Handle cancel (e.g., close the app or disable location-dependent features)
        }
        builder.create().show()
    }


    private fun fetchWeatherDataForLocation(lat: Double, lon: Double) {
        lifecycleScope.launch {
            weatherVM.updateWeatherFromMap(lat, lon)
            observeWeatherData()
        }
    }

    private fun fetchWeatherDataForCurrentLocation() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Use stored latitude and longitude if available
                if (currentLat != null && currentLon != null) {
                    weatherVM.updateWeatherFromMap(currentLat!!, currentLon!!)
                    Log.i("MainActivity", "currentLat: $currentLat, currentLon: $currentLon")
                } else {
                    // Fetch data based on user's current location
                    weatherVM.getWeatherForCurrentLocation()
                }
                // ... error handling ...
            } catch (e: IOException) {
                // ... error handling ...
            } catch (e: Exception) {
                // ... error handling ...
            } finally {
                // ... hide loading indicator ...
            }
        }
    }

    private fun isArabicLanguageSelected(): String? {
        val sharedPreferences = getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("current_locale", "en")
        return languageCode
    }

    private fun observeWeatherData() {
        weatherVM.forecast.observe(this, Observer { forecast ->
            forecast?.let {
                updateUI(it)
                Log.i(
                    "MainActivity",
                    "Forecast received: ${it.city.coord.lat}, ${it.city.coord.lon}"
                )
                val forecast  = getSharedPreferences("myprefs", Context.MODE_PRIVATE).edit()
                forecast.putString("forecast", Gson().toJson(it))
                forecast.apply()

            } ?: ToastUtil.showCustomToast(this, "No weather data available")

        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(forecast: ForCast) {
          isArabic = Locale.getDefault().language == isArabicLanguageSelected()
        Log.i("FromMainLanguageIs", "isArabic: $isArabic")

        forecast.weatherList.let {
            HourFromFragment = it[0].toString()
            Log.i("Time", "UpdateUIIIIIIIIIIIIIIIIIIIIII: $HourFromFragment")
            val dtTxt2 = it[0].dt_txt!!.substring(11, 13).toInt()
            updateBackgroundBasedOnTime(dtTxt2)

        }
        forecast.city.let { city ->
            Log.i("CityFromFragment", "Is : $cityNameFromFragment")
            Log.i("CityFromForecast", "Is : ${city.name}")
            binding.cityText.text = if (cityNameFromFragment != null) cityNameFromFragment else  city.name
            binding.lat.text = "${city.coord.lat}"
            binding.lon.text = "${city.coord.lon}"
            val sharedPreferences = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            selectedUnit = sharedPreferences.getString(TEMP_UNIT_KEY, "Celsius") ?: "Celsius"

            setupAdapters(selectedUnit)

            Log.i("MainActivity", "unittttttt name: $selectedUnit")
            val Temp = forecast.weatherList[0].main.temp

            if (!isArabic) {
                binding.Temp.text =
                    when (selectedUnit) {
                        "Celsius" -> ToastUtil.formatNumberToArabic(
                            ToastUtil.convertToCelsius(Temp).roundToInt()
                        ) + getString(
                            R.string.C
                        )

                        "Fahrenheit" ->
                            ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToFahrenheit(Temp).roundToInt()
                            ) + getString(
                                R.string.F
                            )

                        else -> ToastUtil.formatNumberToArabic(
                            ToastUtil.convertToKelvin(Temp).roundToInt()
                        ) + getString(
                            R.string.K
                        )
                    }
            } else {

                //English 3adi
                binding.Temp.text = when (selectedUnit) {
                    "Celsius" -> "${ToastUtil.convertToCelsius(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                        R.string.C
                    )

                    "Fahrenheit" -> "${
                        ToastUtil.convertToFahrenheit(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                        R.string.F
                    )

                    else -> "${ToastUtil.convertToKelvin(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                        R.string.K
                    )
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
                if (key == TEMP_UNIT_KEY) {
                    selectedUnit = sharedPreferences.getString(TEMP_UNIT_KEY, "Celsius") ?: "Celsius"
                    Log.i("LanguageFromMain", "Lang: $isArabic")
                    if (!isArabic) {
                        when (selectedUnit) {
                            "Celsius" -> binding.Temp.text =
                                ToastUtil.formatNumberToArabic(
                                    ToastUtil.convertToCelsius(forecast.weatherList[0].main.temp)
                                        .roundToInt()
                                ) + getString(R.string.C)

                            "Fahrenheit" -> binding.Temp.text =
                                ToastUtil.formatNumberToArabic(
                                    ToastUtil.convertToFahrenheit(
                                        forecast.weatherList[0].main.temp
                                    ).roundToInt()
                                ) + getString(R.string.F)

                            else -> binding.Temp.text =
                                ToastUtil.formatNumberToArabic(
                                    ToastUtil.convertToKelvin(forecast.weatherList[0].main.temp)
                                        .roundToInt()
                                ) + getString(R.string.K)

                        }
                    } else {
                        binding.Temp.text = when (selectedUnit) {
                            "Kelvin" -> "${
                                ToastUtil.convertToKelvin(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                                R.string.K
                            )

                            "Fahrenheit" -> "${
                                ToastUtil.convertToFahrenheit(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                                R.string.F
                            )

                            else -> "${forecast.weatherList[0].main.temp.roundToInt()} +${
                                getString(
                                    R.string.C
                                )
                            }"
                        }
                    }
                }
            }

            val feelsLikeTranslated =
                forecast.weatherList[0].main.feels_like

            val H = ToastUtil.translateWeatherDescription(
                forecast.weatherList[0].main.temp_max.toString(),
                this
            ).toDouble()
            val L = ToastUtil.translateWeatherDescription(
                forecast.weatherList[0].main.temp_min.toString(),
                this
            ).toDouble()

            if (!isArabic) {

                when (selectedUnit) {
                    "Celsius" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " + ToastUtil.formatNumberToArabic(H.toInt()) + getString(
                                R.string.C
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + ToastUtil.formatNumberToArabic(L.toInt()) + getString(
                                R.string.C
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ": " + ToastUtil.formatNumberToArabic(
                                feelsLikeTranslated.toInt()
                            ) + getString(
                                R.string.C
                            )

                    }

                    "Fahrenheit" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToFahrenheit(
                                    H
                                ).toInt()
                            ) + getString(
                                R.string.F
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToFahrenheit(
                                    L
                                ).toInt()
                            ) + getString(
                                R.string.F
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToFahrenheit(feelsLikeTranslated).toInt()
                            ) + getString(R.string.F)

                    }

                    else -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToKelvin(
                                    H
                                ).toInt()
                            ) + getString(
                                R.string.K
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToKelvin(
                                    L
                                ).toInt()
                            ) + getString(
                                R.string.K
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToKelvin(feelsLikeTranslated).toInt()
                            ) + getString(R.string.K)

                    }
                }

            } else {
                when (selectedUnit) {
                    "Celsius" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + ": " + "${H.roundToInt()}" + getString(R.string.C)
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + "${L.roundToInt()}" + getString(R.string.C)
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ": " + "${forecast.weatherList[0].main.feels_like.roundToInt()}" + getString(
                                R.string.C
                            )
                    }

                    "Fahrenheit" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + ": " + "${ToastUtil.convertToFahrenheit(H).roundToInt()}" + getString(
                                R.string.F
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + "${ToastUtil.convertToFahrenheit(L).roundToInt()}" + getString(
                                R.string.F
                            )
                        binding.tempFeelsLike.text = this.getString(R.string.feelsLike) + ": " + "${
                            ToastUtil.convertToFahrenheit(forecast.weatherList[0].main.feels_like).roundToInt()
                        }" + getString(R.string.F)
                    }

                    else -> {
                        binding.tempMax.text =
                            getString(R.string.H) + ": " + "${ToastUtil.convertToKelvin(H).roundToInt()}" + getString(
                                R.string.K
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + "${ToastUtil.convertToKelvin(L).roundToInt()}" + getString(
                                R.string.K
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ": " + "${
                                ToastUtil.convertToKelvin(
                                    forecast.weatherList[0].main.feels_like
                                ).roundToInt()}" + getString(
                                R.string.K
                            )
                    }
                }
            }


            val descriptionTranslated =
                ToastUtil.translateWeatherDescription(
                    forecast.weatherList[0].weather[0].description,
                    this
                )
            binding.Descriptions.text = descriptionTranslated
            Log.i(
                "MainActivity",
                "City name: ${city.name} , Description: ${forecast.weatherList[0].weather[0].description}"
            )

        }

        if (forecast.weatherList.isNotEmpty()) {
            val currentWeather = forecast.weatherList[0]
            val iconCode = forecast.weatherList[0].weather[0].icon
            val sunrise = forecast.city.sunrise * 1000L // Convert to milliseconds
            val sunset = forecast.city.sunset * 1000L // Convert to milliseconds
            Log.i("MainActivity", "Sunrise: $sunrise, Sunset: $sunset")

            val currentTime = System.currentTimeMillis()
            val isDaytime = currentTime in sunrise..sunset

            updateWeatherDetails(currentWeather)
            binding.hourlyRec.adapter =
                WeatherTodayAdapter(forecast.weatherList.take(8), selectedUnit)
            binding.dailyRec.adapter = DailyAdapter(forecast.weatherList, selectedUnit)
            val iconResource = when {
                isDaytime -> {
                    binding.main.setBackgroundResource(R.drawable.clearskybackground) // Set daytime background
                    when (iconCode) {
                        "01d" -> R.drawable.sun // Clear sky during the day
                        "01n" -> R.drawable.moon // Clear sky at night
                        "02d", "02n" -> R.drawable.cloudyday // Partly cloudy
                        "03d", "03n" -> R.drawable.plaincouds // Scattered clouds
                        "04d", "04n" -> R.drawable.four // Overcast clouds
                        "09d", "09n" -> R.drawable.rainy // Rain
                        "10d", "10n" -> R.drawable.cloudy // Cloudy with rain
                        "11d", "11n" -> R.drawable.thunder // Thunderstorm
                        "13d", "13n" -> R.drawable.snowflake // Snow
                        else -> R.drawable.sun // Default icon
                    }
                }

                else -> {
                    // Set the background based on the icon code during nighttime
                    when (iconCode) {
                        "01n" -> {
                            binding.main.setBackgroundResource(R.drawable.clearskynight)
                            R.drawable.moon
                        }

                        "02n" -> R.drawable.cloud.also {
                            binding.main.setBackgroundResource(R.drawable.clearskynight)
                        }

                        "03n" -> R.drawable.plaincouds // Scattered clouds
                        "04n" -> R.drawable.four // Overcast clouds
                        "09n" -> R.drawable.rainy // Rain
                        "10n" -> R.drawable.cloudy // Cloudy with rain
                        "11n" -> R.drawable.thunder // Thunderstorm
                        "13n" -> R.drawable.snowflake // Snow
                        else -> R.drawable.moon // Default icon
                    }
                }
            }

            binding.icon.setImageResource(iconResource)

            binding.lottieAnimationView.visibility = View.GONE


            binding.Linear.visibility = View.VISIBLE
            binding.mainLayout.visibility = View.VISIBLE
        } else {
            ToastUtil.showCustomToast(this, "No weather data available")
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun updateBackgroundBasedOnTime(hourOfDayString: Int) {
        Log.i("Date", "updateBackground: $hourOfDayString")
        var Timing = hourOfDayString

        Log.i("TimeFromTheFragmentInMAAAIN", "Time : $Timing")
        Timing.let {
            val isPm = it >= 12
            val normalHour = when {
                it == 0 -> 12      // Handle midnight case
                it == 12 -> 12     // Handle noon case
                it > 12 -> it - 12 // For PM times, subtract 12 to convert
                else -> it          // For AM times, no change needed
            }
            val amPm = if (isPm) "PM" else "AM"
            val formattedTime = "$normalHour:00 $amPm"
            Log.i("AM/PM", "Time in 12-hour format: $formattedTime")
            "$normalHour:00 $amPm"
            Log.i("AM/PM", "Time in 12-hour format: $normalHour:00 $amPm")

        }
        Log.i("TimeFromTheFragmentInMAAAIN", "Time : $Timing")

        // Define the start and end hours for day time
        val DAY_START_HOUR = 6  // 6 AM as a string
        val DAY_END_HOUR =   18   // 5 PM as a string
        Log.i("Time Range", "Checking if $Timing is between $DAY_START_HOUR and $DAY_END_HOUR")

        val isDayTime = Timing in DAY_START_HOUR..DAY_END_HOUR
        Log.i("Time Check", "Is Day Time? $isDayTime")

        binding.main.setBackgroundResource(
            if (isDayTime) R.drawable.clearskybackground
            else
                R.drawable.clearskynight

        )
        binding.Linear.setBackgroundResource(
            if (isDayTime) R.drawable.clearskybackground
            else R.drawable.clearskynight
        )

    }



    private fun updateLocale() {
        val sharedPreferences = getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getBoolean("isArabic", false) // Default to English
        val savedLocale = if (languageCode) "ar" else "en"

        val locale = Locale(savedLocale)
        Locale.setDefault(locale)

        val resources = resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun updateWeatherDetails(currentWeather: WeatherList) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
         val  WindUnit = sharedPrefs.getString(WIND_UNIT_KEY, getString(R.string.Km)) ?: getString(R.string.Km)
        Log.i("WindUnitInMain1", "WindUnit: $WindUnit")
       isArabic = Locale.getDefault().language == isArabicLanguageSelected()


        if (!isArabic) {

            binding.windval.text = when (WindUnit) {
                "MPH" -> "${ToastUtil.formatNumberToArabic(currentWeather.wind.speed.toInt()).format(".")} " + getString(
                    R.string.mph
                )

                else -> "${ToastUtil.formatNumberToArabic(currentWeather.wind.speed.toInt()).format(".")} " + getString(
                    R.string.Km
                )
            }
        } else {
            binding.windval.text = when (WindUnit) {
                "MPH" -> "${ToastUtil.convertToMPH(currentWeather.wind.speed).toInt()}" + getString(
                    R.string.mph
                )
                else -> (currentWeather.wind.speed.toInt()).toString() + getString(R.string.Km)
            }

        }
        //Update temp unit when preferences change
        sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == WIND_UNIT_KEY) {
               val WindUnit =
                    sharedPrefs.getString(WIND_UNIT_KEY, getString(R.string.mph)) ?: getString(R.string.mph)
         Log.i("WindUnitInMain2", "WindUnit: $WindUnit")
                if (!isArabic) {
                    binding.windval.text = when (WindUnit) {

                        "MPH" -> ToastUtil.convertToMPH(currentWeather.wind.speed).toString().format(".")+ getString(
                            R.string.mph
                        )
                        else -> (currentWeather.wind.speed).toString().format(".") + getString(R.string.Km)

                    }
                }else{
                    binding.windval.text = when (WindUnit) {
                        "MPH" -> "${
                            ToastUtil.formatNumberToArabic(
                                ToastUtil.convertToMPH(
                                    currentWeather.wind.speed
                                ).toInt()
                            )
                        }" + getString(R.string.mph)
                        else -> "${ToastUtil.formatNumberToArabic((currentWeather.wind.speed).toInt())}" + getString(
                            R.string.Km
                        )
                    }
                }
            }
        }
        with(binding) {

            if (isArabic) {
                Log.i("LANGUAGE", "ISNNOTARABIC: $isArabic")
                pressureval.text = currentWeather.main.pressure.toString()
                humidityval.text = currentWeather.main.humidity.toString()
                visibilityval.text = currentWeather.visibility.toString()
                airqualityval.text = currentWeather.main.temp.toString()  // Placeholder
                uvval.text = currentWeather.main.sea_level.toString()
            } else {
                Log.i("LANGUAGE", "LAnguageISARABICNOW: $isArabic")
                pressureval.text = ToastUtil.formatNumberToArabic(currentWeather.main.pressure)
                humidityval.text = ToastUtil.formatNumberToArabic(currentWeather.main.humidity)
                visibilityval.text = ToastUtil.formatNumberToArabic(currentWeather.visibility)
                airqualityval.text =
                    ToastUtil.formatNumberToArabic(currentWeather.main.temp.toInt())// Placeholder
                uvval.text =
                    ToastUtil.formatNumberToArabic(currentWeather.main.sea_level) // Placeholder// Placeholder
            }
            humiditygraphic.setImageResource(R.drawable.humidity)
            PressureGraphic.setImageResource(R.drawable.pressure)
            windGraphic.setImageResource(R.drawable.wind)
            visibilitygraphic.setImageResource(R.drawable.visibility)
            airqualitygraphic.setImageResource(R.drawable.airquality)
            uvgraphic.setImageResource(R.drawable.uv)
        }

    }

    private fun getSelectedLocation(): Pair<Double, Double>? {
        val sharedPreferences = getSharedPreferences("locationResult", Context.MODE_PRIVATE)

        val lat = sharedPreferences.getFloat("latitude", 0f).toDouble()
        val lon = sharedPreferences.getFloat("longitude", 0f).toDouble()
Log.i("FROMGETSELECTEDLOCATION", "Received latitude: $lat, longitude: $lon")
        return if (lat != 0.0 && lon != 0.0) {
            Pair(lat, lon)
        } else {
            null
        }
    }
    override fun onStart() {
        super.onStart()
        Log.i("ONSTARTTTTTTTTTTT", "onStart")
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkStateReceiver, intentFilter)
        lifecycleScope.launch(Dispatchers.Main) {
                if (NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                    isConnected = true
                    // Network is available
                    if (!checkedPermissions()) {
                        // Request permissions if not granted
                        promptEnableLocationServices()
                    }


        }else {
                    isConnected = false
                    handleNetworkChange(isConnected)
                        // Show no internet layout and display the snackbar
                    lifecycleScope.launch {
                        binding.lottieAnimationView.visibility = View.VISIBLE
                        delay(5000)
                        showNoInternetLayout(false) // Show the no internet layout
                        binding.lottieAnimationView.visibility = View.GONE


                    }
                }
                }
            }


    override fun onStop() {
        super.onStop()
        weatherVM.loadCachedWeather()
        Log.i("ONRESUME", "onResume")
    }

    private fun showConnectToInternetDialog():AlertDialog{
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)

        // Set title or message if needed
        builder.setTitle("Connect To Internet")
        builder.setMessage("You have to connect to the internet to see your weather status in real time")

        // Update location button
        builder.setPositiveButton("Go To Settings ") { _, _ ->
            // Handle the update location action
           Intent(Settings.ACTION_WIFI_SETTINGS).also {
               startActivity(it)
           }

            Toast.makeText(this, "Navigating to settings  ", Toast.LENGTH_SHORT).show()
        }

        // Just navigate button
        builder.setNegativeButton("Cancel") { dialog, id ->
            lifecycleScope.launch {
                binding.CahcedText.visibility=View.VISIBLE
                delay(2000)
                binding.CahcedText.visibility=View.GONE
                delay(1000)

showSnackbar(getString(R.string.cachedSnackBar))
            }
            offlineContent()
        }
        // Cancel button


        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.show()

        return dialog

    }
    private fun  showNoInternetLayout(loading: Boolean = true) {
        if (loading) {
            binding.main.visibility = View.GONE
            binding.noInternetConstraint.visibility = View.VISIBLE
            binding.lottieAnimationView.visibility = View.VISIBLE
        } else {
            binding.main.visibility = View.VISIBLE
            binding.noInternetConstraint.visibility = View.GONE

        }
    }

    private fun checkedPermissions(): Boolean {
        val isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d("Permissions", "Coarse location permission granted: $isCoarseLocationPermissionGranted")
        Log.d("Permissions", "Fine location permission granted: $isFineLocationPermissionGranted")
        // Check if location services are enabled
        val isLocationEnabled = promptEnableLocationServices()

        return (isFineLocationPermissionGranted || isCoarseLocationPermissionGranted) && isLocationEnabled
    }

    private fun promptEnableLocationServices(): Boolean {
        // Get the system's location service
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            // Location services are disabled, show the dialog to prompt the user to enable them
            showLocationEnableDialog()
        }
           requestLocationPermission()

        // Return true if either GPS or Network provider is enabled
        return isGpsEnabled || isNetworkEnabled
    }
}

