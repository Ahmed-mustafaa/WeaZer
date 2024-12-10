package com.example.weatherapp

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
import android.app.usage.NetworkStatsManager
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.example.weatherapp.adapter.DailyAdapter
import com.example.weatherapp.adapter.WeatherTodayAdapter
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.model.WeatherList
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

    private var isArabic = Locale.getDefault().language == "ar"

    private lateinit var sharedPrefsHelper: SharedPrefs
    private lateinit var networkStateReceiver: NetworkStateReceiver
    private var isConnected = true
    private var connectionDialog: AlertDialog? = null


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
        val navigationView : NavigationView = binding.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Home_title -> {
                    binding.toolbar.title = getString(R.string.Home)
                    binding.mainLayout.visibility = View.VISIBLE
                    binding.navHostFragment.visibility = View.GONE
                    binding.navigationView.visibility = View.GONE
                    navController.popBackStack()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.Alarm_title -> {
                    // Navigate to Alarm Fragment
                    binding.toolbar.title = getString(R.string.Alarms)
                    binding.back.visibility = View.VISIBLE

                    binding.mainLayout.visibility = View.GONE
                    /*
                    binding.fragmentContainerView.visibility = View.VISIBLE
*/
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navigationView.visibility = View.VISIBLE

                    navController.navigate(R.id.Alarm)
                    drawerLayout.closeDrawer(GravityCompat.START)

                    true
                }

                R.id.Favorites_title -> {
                    // Navigate to Favorites
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
                    true
                }

                R.id.Settings_title -> {
                    binding.toolbar.title = getString(R.string.Settings)
                    binding.back.visibility = View.VISIBLE

                    binding.mainLayout.visibility = View.GONE
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navigationView.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()

                    navController.navigate(R.id.settings)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true

                }

                else -> false
            }
        }
        setupViewModel()

        if(!NetworkUtils.isNetworkAvailable(this)){

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
                  delay(300)
                  showSnackbar(getString(R.string.connectionRestored))
                  binding.swipeRefreshLayout.isRefreshing = false
                  connectionDialog?.dismiss()
                  handleInitialLocation()
                  observeWeatherData()
              }
            } else {
                // Handle no connection scenario
                lifecycleScope.launch {
                    val job = Job()
                    connectionDialog = showConnectToInternetDialog()
                    if (connectionDialog!!.isShowing) {
                        connectionDialog?.setOnDismissListener {
                            lifecycleScope.launch {
                                binding.CahcedText.apply {
                                    visibility = View.VISIBLE
                                    binding.noInternetConstraint.visibility = View.GONE
                                    delay(500)
                                    binding.CahcedText.visibility = View.GONE
                                }
                            }

                        }
                        job.cancel()
                        job.invokeOnCompletion {
                            showNoInternetLayout(false)
                            lifecycleScope.launch {
                                weatherVM.loadCachedWeather()
                                observeWeatherData() // Fetch cached data
                                Log.i("OnCreate", "Displaying Cached Data")
                                showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                                showSnackbar(getString(R.string.cachedSnackBar))
                            }
                        }
                    }
                }
            }
        }


        setupAdapters(selectedUnit)
        requestNotificationPermission(this)
        setupAlarm()
        requestBatteryOptimizationPermission()


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
            val sharedPreferencesCity = this@MainActivity.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
            sharedPreferencesCity.edit().remove("fullLocation").apply()
            cityNameFromFragment = null
            lifecycleScope.launch {
                if (NetworkUtils.isNetworkAvailable(this@MainActivity)) {

                    val savedLocation = getSelectedLocation()
                    if (savedLocation != null) {
                        val (lat, lon) = savedLocation
                        fetchWeatherDataForLocation(
                            lat,
                            lon
                        ) // Fetch data for saved location

                    } else {
                        fetchWeatherDataForCurrentLocation()

                    }


                } else {
                    showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                binding.swipeRefreshLayout.isRefreshing = false


            }
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
            binding.lottieAnimationView.visibility = View.VISIBLE

        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationPermission() {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }




    @SuppressLint("ScheduleExactAlarm")
    private fun setupAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("ALarmFromMAin", "This means the SDK is Higher >  26  ")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val timeInMillis = System.currentTimeMillis()
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
                    } else {
                        Log.e("MainActivity", "Failed to retrieve location.")
                    }

                }
            }

            }
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
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

            } ?: ToastUtil.showCustomToast(this,"No weather data available")

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
                        "Celsius" -> formatNumberToArabic(convertToCelsius(Temp).roundToInt()) + getString(
                            R.string.C
                        )

                        "Fahrenheit" ->
                            formatNumberToArabic(convertToFahrenheit(Temp).roundToInt()) + getString(
                                R.string.F
                            )

                        else -> formatNumberToArabic(convertToKelvin(Temp).roundToInt()) + getString(
                            R.string.K
                        )
                    }
            } else {

                //English 3adi
                binding.Temp.text = when (selectedUnit) {
                    "Celsius" -> "${convertToCelsius(forecast.weatherList[0].main.temp).roundToInt()}" + getString(R.string.C)

                    "Fahrenheit" -> "${convertToFahrenheit(forecast.weatherList[0].main.temp).roundToInt()}" + getString(R.string.F)

                    else -> "${convertToKelvin(forecast.weatherList[0].main.temp).roundToInt()}" + getString(R.string.K)
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
                if (key == TEMP_UNIT_KEY) {
                    selectedUnit = sharedPreferences.getString(TEMP_UNIT_KEY, "Celsius") ?: "Celsius"
                    Log.i("LanguageFromMain", "Lang: $isArabic")
                    if (!isArabic) {
                        when (selectedUnit) {
                            "Celsius" -> binding.Temp.text =
                                formatNumberToArabic(convertToCelsius(forecast.weatherList[0].main.temp).roundToInt()) + getString(R.string.C)

                            "Fahrenheit" -> binding.Temp.text =
                                formatNumberToArabic(convertToFahrenheit(forecast.weatherList[0].main.temp).roundToInt()) + getString(R.string.F)

                            else -> binding.Temp.text =
                                formatNumberToArabic(convertToKelvin(forecast.weatherList[0].main.temp).roundToInt()) + getString(R.string.K)

                        }
                    } else {
                        binding.Temp.text = when (selectedUnit) {
                            "Kelvin" -> "${convertToKelvin(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
                                R.string.K
                            )

                            "Fahrenheit" -> "${convertToFahrenheit(forecast.weatherList[0].main.temp).roundToInt()}" + getString(
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

            val H = translateWeatherDescription(
                forecast.weatherList[0].main.temp_max.toString(),
                this
            ).toDouble()
            val L = translateWeatherDescription(
                forecast.weatherList[0].main.temp_min.toString(),
                this
            ).toDouble()

            if (!isArabic) {

                when (selectedUnit) {
                    "Celsius" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " +formatNumberToArabic( H.toInt()) + getString(R.string.C)
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + formatNumberToArabic(L.toInt()) + getString(
                                R.string.C
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ": " + formatNumberToArabic(feelsLikeTranslated.toInt())+ getString(
                                R.string.C
                            )

                    }

                    "Fahrenheit" -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " + formatNumberToArabic(
                                convertToFahrenheit(
                                    H
                                ).toInt()
                            ) + getString(
                                R.string.F
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + formatNumberToArabic(
                                convertToFahrenheit(
                                    L
                                ).toInt()
                            ) + getString(
                                R.string.F
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + formatNumberToArabic(
                                convertToFahrenheit(feelsLikeTranslated).toInt()
                            ) + getString(R.string.F)

                    }

                    else -> {
                        binding.tempMax.text =
                            getString(R.string.H) + " : " + formatNumberToArabic(convertToKelvin(H).toInt()) + getString(
                                R.string.K
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + formatNumberToArabic(convertToKelvin(L).toInt()) + getString(
                                R.string.K
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + formatNumberToArabic(
                                convertToKelvin(feelsLikeTranslated).toInt()
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
                            getString(R.string.H) + ": " + "${convertToFahrenheit(H).roundToInt()}" + getString(
                                R.string.F
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + "${convertToFahrenheit(L).roundToInt()}" + getString(
                                R.string.F
                            )
                        binding.tempFeelsLike.text = this.getString(R.string.feelsLike) + ": " + "${
                            convertToFahrenheit(forecast.weatherList[0].main.feels_like).roundToInt()
                        }" + getString(R.string.F)
                    }

                    else -> {
                        binding.tempMax.text =
                            getString(R.string.H) + ": " + "${convertToKelvin(H).roundToInt()}" + getString(
                                R.string.K
                            )
                        binding.tempMin.text =
                            getString(R.string.L) + " : " + "${convertToKelvin(L).roundToInt()}" + getString(
                                R.string.K
                            )
                        binding.tempFeelsLike.text =
                            this.getString(R.string.feelsLike) + ": " + "${convertToKelvin(forecast.weatherList[0].main.feels_like).roundToInt()}" + getString(
                                R.string.K
                            )
                    }
                }
            }


            val descriptionTranslated =
                translateWeatherDescription(forecast.weatherList[0].weather[0].description, this)
            binding.Descriptions.text = descriptionTranslated
            Log.i(
                "MainActivity",
                "City name: ${city.name} , Description: ${forecast.weatherList[0].weather[0].description}"
            )

        }

        if (forecast.weatherList.isNotEmpty()) {
            val currentWeather = forecast.weatherList[0]
            updateWeatherDetails(currentWeather)
            binding.hourlyRec.adapter =
                WeatherTodayAdapter(forecast.weatherList.take(8), selectedUnit)
            binding.dailyRec.adapter = DailyAdapter(forecast.weatherList, selectedUnit)
            when (forecast.weatherList[0].weather[0].icon) {
                "01d" -> binding.icon.setImageResource(R.drawable.sun)
                "01n" -> binding.icon.setImageResource(R.drawable.moon)
                "02d" -> binding.icon.setImageResource(R.drawable.clouds)
                "02n" -> binding.icon.setImageResource(R.drawable.towon)
                "03d", "03n" -> binding.icon.setImageResource(R.drawable.plaincouds)
                "04d", "04n" -> binding.icon.setImageResource(R.drawable.four)
                "09d", "09n" -> binding.icon.setImageResource(R.drawable.rainy)
                "10d", "10n" -> binding.icon.setImageResource(R.drawable.cloudy)
                "11d", "11n" -> binding.icon.setImageResource(R.drawable.thunder)
                "13d", "13n" -> binding.icon.setImageResource(R.drawable.snowflake)
                else -> binding.icon.setImageResource(R.drawable.sun) // A default icon
            }
            binding.lottieAnimationView.visibility = View.GONE


            binding.Linear.visibility = View.VISIBLE
            binding.mainLayout.visibility = View.VISIBLE
        } else {
            ToastUtil.showCustomToast(this,"No weather data available")
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun updateBackgroundBasedOnTime(hourOfDayString: Int) {
        Log.i("Date", "updateBackground: $hourOfDayString")
        var Timing = hourOfDayString

        Log.i("TimeFromTheFragmentInMAAAIN", "Time : $Timing")
        Timing.let {
            val isPm = it >= 12
            val normalHour = if (it % 12 == 0) 12 else it % 12
            val amPm = if (isPm) "PM" else "AM"
            "$normalHour:00 $amPm"
            Log.i("AM/PM", "Time in 12-hour format: $normalHour:00 $amPm")

        }
        Log.i("TimeFromTheFragmentInMAAAIN", "Time : $Timing")

        // Define the start and end hours for day time
        val DAY_START_HOUR = 6  // 6 AM as a string
        val DAY_END_HOUR =   18   // 6 PM as a string
        Log.i("Time Range", "Checking if $Timing is between $DAY_START_HOUR and $DAY_END_HOUR")

        val isDayTime = Timing in DAY_START_HOUR..DAY_END_HOUR
        Log.i("Time Check", "Is Day Time? $isDayTime")

        binding.main.setBackgroundResource(
            if (isDayTime) R.drawable.clearskybackground
            else R.drawable.clearskynight
        )
        binding.Linear.setBackgroundResource(
            if (isDayTime) R.drawable.clearskybackground
            else R.drawable.clearskynight
        )

    }


    private fun convertToCelsius(temp: Double): Double {
        return temp
    }

    private fun convertToFahrenheit(temp: Double): Double {
        return (temp * 9 / 5) + 32
    }

    private fun convertToKelvin(temp: Double): Double {
        return (temp - 32) * 5 / 9 + 273.15
    }

    private fun convertToMPH(speed: Double): Double {
        return speed * 0.621371
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

    private  fun translateWeatherDescription(description: String, context: Context): String {
        return when (description.toLowerCase(Locale.getDefault())) {
            "clear sky" -> context.getString(R.string.clear_sky)
            "few clouds" -> context.getString(R.string.few_clouds)
            "scattered clouds" -> context.getString(R.string.scattered_clouds)
            "overcast clouds" -> context.getString(R.string.overcast_clouds)
            "rain" -> context.getString(R.string.rain)
            "thunderstorm" -> context.getString(R.string.thunderstorm)
            "snow" -> context.getString(R.string.snow)
            "mist" -> context.getString(R.string.mist)
            "smoke" -> context.getString(R.string.smoke)
            "haze" -> context.getString(R.string.haze)
            "dust" -> context.getString(R.string.dust)
            "fog" -> context.getString(R.string.fog)
            "sand" -> context.getString(R.string.sand)
            "ash" -> context.getString(R.string.ash)
            "squall" -> context.getString(R.string.squall)
            "tornado" -> context.getString(R.string.tornado)
            "light rain" -> context.getString(R.string.light_rain)
            "moderate rain" ->context.getString(R.string.moderate_rain)
            "heavy intensity rain" -> context.getString(R.string.heavy_intensity_rain)
            "very heavy rain" -> context.getString(R.string.very_heavy_rain)
            "broken clouds" -> context.getString(R.string.broken_clouds)
            "feelsLike" -> context.getString(R.string.feelsLike)
            "H" -> context.getString(R.string.H)
            "L" -> context.getString(R.string.L)


            else -> description // Return the description as-is if no translation exists
        }
    }


    private fun updateWeatherDetails(currentWeather: WeatherList) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
         val  WindUnit = sharedPrefs.getString(WIND_UNIT_KEY, getString(R.string.Km)) ?: getString(R.string.Km)
        Log.i("WindUnitInMain1", "WindUnit: $WindUnit")
       isArabic = Locale.getDefault().language == isArabicLanguageSelected()


        if (!isArabic) {

            binding.windval.text = when (WindUnit) {
                "MPH" -> "${formatNumberToArabic(currentWeather.wind.speed.toInt()).format(".")} " + getString(R.string.mph)

                else -> "${formatNumberToArabic(currentWeather.wind.speed.toInt()).format(".")} " + getString(R.string.Km)
            }
        } else {
            binding.windval.text = when (WindUnit) {
                "MPH" -> "${convertToMPH(currentWeather.wind.speed).toInt()}" + getString(R.string.mph)
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

                        "MPH" -> convertToMPH(currentWeather.wind.speed).toString().format(".")+ getString(R.string.mph)
                        else -> (currentWeather.wind.speed).toString().format(".") + getString(R.string.Km)

                    }
                }else{
                    binding.windval.text = when (WindUnit) {
                        "MPH" -> "${formatNumberToArabic(convertToMPH(currentWeather.wind.speed).toInt())}" + getString(R.string.mph)
                        else -> "${formatNumberToArabic((currentWeather.wind.speed).toInt())}" + getString(R.string.Km)
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
                pressureval.text = formatNumberToArabic(currentWeather.main.pressure)
                humidityval.text = formatNumberToArabic(currentWeather.main.humidity)
                visibilityval.text = formatNumberToArabic(currentWeather.visibility)
                airqualityval.text = formatNumberToArabic(currentWeather.main.temp.toInt())// Placeholder
                uvval.text = formatNumberToArabic(currentWeather.main.sea_level) // Placeholder// Placeholder
            }
            humiditygraphic.setImageResource(R.drawable.humidity)
            PressureGraphic.setImageResource(R.drawable.pressure)
            windGraphic.setImageResource(R.drawable.wind)
            visibilitygraphic.setImageResource(R.drawable.visibility)
            airqualitygraphic.setImageResource(R.drawable.airquality)
            uvgraphic.setImageResource(R.drawable.uv)
        }

    }


    private  fun formatNumberToArabic(number: Int): String {
        return number.toString().map { char ->
            when (char) {
                '0' -> '٠'
                '1' -> '١'
                '2' -> '٢'
                '3' -> '٣'
                '4' -> '٤'
                '5' -> '٥'
                '6' -> '٦'
                '7' -> '٧'
                '8' -> '٨'
                '9' -> '٩'
                else -> char // Return the character as is if it's not a digit
            }
        }.joinToString("")
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
                        showNoInternetLayout(true) // Show the no internet layout
                        showSnackbar(getString(R.string.Please_check_your_internet_connection_and_try_again))
                    }
                }
            }

    override fun onResume() {
        super.onResume()
        val sharedPreferencesCity = this.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val cityNameFromSharedPrefs = sharedPreferencesCity.getString("fullLocation", null)
        if (cityNameFromSharedPrefs != null) {
            val updatedCityName = sharedPreferencesCity.getString("fullLocation", null)
            cityNameFromFragment = updatedCityName
            Log.i("FROMONRESUME", "cityNameFromFragment: $cityNameFromFragment")
        } else {
            cityNameFromFragment = null
            Log.i("FROMONRESUME", "cityNameFromFragment: $cityNameFromFragment is null already ")
        }
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
            // Handle the just navigate action
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
            this.recreate()

        }
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkStateReceiver)
    }

    private fun checkedPermissions(): Boolean {
        val isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notificationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        Log.d("Permissions", "Coarse location permission granted: $isCoarseLocationPermissionGranted")
        Log.d("Permissions", "Fine location permission granted: $isFineLocationPermissionGranted")
        // Check if location services are enabled
        val isLocationEnabled = promptEnableLocationServices()

        return (isFineLocationPermissionGranted || isCoarseLocationPermissionGranted) && notificationPermissionGranted && isLocationEnabled
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

