package com.example.weatherapp

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.adapter.DailyAdapter
import com.example.weatherapp.adapter.ForeCastAdapter
import com.example.weatherapp.adapter.WeatherTodayAdapter
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.model.ForCast
import com.example.weatherapp.model.WeatherList
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherVM: WeatherVM
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private val DAY_START_HOUR = 6
    private val DAY_END_HOUR = 18
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.main
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        val navigationView: NavigationView = binding.navigationView
        binding.mainLayout.visibility = View.VISIBLE
        binding.fragmentContainerView.visibility = View.GONE


        val drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Alarm_title -> {
                    // Navigate to Alarm Fragment
                    binding.mainLayout.visibility = View.GONE
                    binding.fragmentContainerView.visibility = View.VISIBLE
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navigationView.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, AlarmScreen())
                        .addToBackStack(null)
                        .commit()
                    navController.navigate(R.id.Alarm)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.Favorites_title -> {
                    // Navigate to Favorites
                    binding.mainLayout.visibility = View.GONE
                    binding.fragmentContainerView.visibility = View.VISIBLE
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navigationView.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, FavoritesFragment())
                        .addToBackStack(null)
                        .commit()
                    navController.navigate(R.id.favorites)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.Settings_title -> {
                    binding.mainLayout.visibility = View.GONE
                    binding.fragmentContainerView.visibility = View.VISIBLE
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navigationView.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                    navController.navigate(R.id.settings)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true

                }

                else -> false
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        setupViewModel()
        setupAdapters()
        handleInitialLocation()
        setupFragmentResultListener()
        requestNotificationPermission(this)
        setupAlarm()
        binding.pickuplocation.setOnClickListener {
            navigateToMapFragment()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 1. Trigger weather data refresh here
            fetchWeatherDataForCurrentLocation()

            // 2. Stop the refreshing animation
            binding.swipeRefreshLayout.isRefreshing = false
        }        /*binding.favorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }*/

        /* binding.addToFav.setOnClickListener {
            val cityName = binding.cityText.text.toString()

            if (cityName.isNotEmpty()) {
                // Check if latitude and longitude are available
                if (currentLat != null && currentLon != null) {
                    showToast("$cityName added to favorites with coordinates ($currentLat, $currentLon)")
                   addFavoriteCityToSharedPreferences(cityName, currentLat.toString(), currentLon.toString())
                    // Pass the data directly to FavoritesActivity
                    val intent = Intent(this, FavoritesActivity::class.java).apply {
                        putExtra("City_name", cityName)
                        putExtra("Lat", currentLat!!) // Pass latitude
                        putExtra("Lon", currentLon!!) // Pass longitude
                    }
                    startActivity(intent)
                } else {
                    showToast("Coordinates are not available yet.")
                }
            } else {
                showToast("City name cannot be empty")
            }
        }*/


    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setupAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // Set the alarm to trigger in 1 minute for testing purposes
        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }.timeInMillis

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("MainActivity", "Alarm set to trigger in 1 minute")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun addFavoriteCityToSharedPreferences(cityName: String, lat: String, lon: String) {
        val sharedPrefs = SharedPrefs.getInstance(this)
        val favoriteCitiesSet = sharedPrefs.getCities()?.toMutableSet() ?: mutableSetOf()

        favoriteCitiesSet.add(cityName)
        sharedPrefs.setCities(favoriteCitiesSet)
        sharedPrefs.addCity(cityName, lat, lon)
        /*
        showToast("$cityName added to favorites!")
*/
    }

    private fun requestNotificationPermission(context: Context) {
        // Only request permission if on Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Register the permission request
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
            }
        }
    }

    private fun setupViewModel() {
        val repository = WeatherRepository(RetrofitClient.apiService, this)
        val factory = ViewModelFactory(repository)
        weatherVM = ViewModelProvider(this, factory)[WeatherVM::class.java]
    }

    private fun setupAdapters() {
        binding.hourlyRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dailyRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.hourlyRec.adapter = WeatherTodayAdapter(emptyList())
        binding.dailyRec.adapter = DailyAdapter(emptyList())
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener("locationRequestKey", this) { _, bundle ->
            val newLat = bundle.getDouble("latitude", 0.0)
            val newLon = bundle.getDouble("longitude", 0.0)
            if (newLat != 0.0 && newLon != 0.0) {
                fetchWeatherDataForLocation(newLat, newLon)
            }
        }
    }

    private fun navigateToMapFragment() {
        binding.fragmentContainerView.visibility = View.VISIBLE
        binding.mainLayout.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, MapsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun handleInitialLocation() {
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        Log.i("MainActivity", "Latitude: $latitude, Longitude: $longitude")

        when {
            latitude != 0.0 && longitude != 0.0 ->
                lifecycleScope.launch(Dispatchers.Main) {
                    fetchWeatherDataForLocation(latitude, longitude)
                }

            checkedPermissions() -> fetchWeatherDataForCurrentLocation()
            else -> promptEnableLocationServices()

        }
    }

    private fun fetchWeatherDataForLocation(lat: Double, lon: Double) {

        lifecycleScope.launch {
            weatherVM.updateWeatherFromMap(lat, lon)
            observeWeatherData()
        }
    }

    private fun fetchWeatherDataForCurrentLocation() {

        lifecycleScope.launch(Dispatchers.Main) {
            weatherVM.getWeatherForCurrentLocation()
            observeWeatherData()
        }

    }

    private fun observeWeatherData() {
        weatherVM.forecast.observe(this, Observer { forecast ->
            forecast?.let {
                updateUI(it)
                Log.i(
                    "MainActivity",
                    "Forecast received: ${it.city.coord.lat}, ${it.city.coord.lon}"
                )
                addFavoriteCityToSharedPreferences(
                    it.city.name,
                    it.city.coord.lat.toString(),
                    it.city.coord.lon.toString()
                )
                currentLat = it.city.coord.lat
                currentLon = it.city.coord.lon

            } ?: showToast("No weather data available")

        })
    }

    private fun updateUI(forecast: ForCast) {
        forecast.city?.let { city ->
            binding.cityText.text = city.name
            binding.lat.text = "${city.coord.lat}"
            binding.lon.text = "${city.coord.lon}"
            binding.Temp.text = "${forecast.weatherList[0].main.temp}°C"
            val feelsLikeTranslated = translateWeatherDescription(
                forecast.weatherList[0].main.feels_like.toString(),
                this
            )
            binding.tempFeelsLike.text = this.getString(R.string.feelsLike) + "$feelsLikeTranslated"
            val L =
                translateWeatherDescription(forecast.weatherList[0].main.temp_max.toString(), this)
            val H =
                translateWeatherDescription(forecast.weatherList[0].main.temp_min.toString(), this)
            binding.tempMax.text = "H: ${forecast.weatherList[0].main.temp_max}°C"
            binding.tempMin.text = "L: ${forecast.weatherList[0].main.temp_min}°C"
            val descriptionTranslated =
                translateWeatherDescription(forecast.weatherList[0].weather[0].description, this)
            binding.Descriptions.text = descriptionTranslated
            Log.i(
                "MainActivity",
                "City name: ${city.name} , Description: ${forecast.weatherList[0].weather[0].description}"
            )
        } ?: Log.e("MainActivity", "City is null in forecast!")

        if (forecast.weatherList.isNotEmpty()) {
            val currentWeather = forecast.weatherList[0]
            updateWeatherDetails(currentWeather)
            updateBackground(currentWeather)
            binding.hourlyRec.adapter = WeatherTodayAdapter(forecast.weatherList.take(8))
            binding.dailyRec.adapter = DailyAdapter(forecast.weatherList)
            when (forecast.weatherList.get(0).weather[0].icon) {
                "01d" -> binding.icon.setImageResource(R.drawable.sun)
                "01n" -> binding.icon.setImageResource(R.drawable.moon)
                "02d" -> binding.icon.setImageResource(R.drawable.clouds)
                "02n" -> binding.icon.setImageResource(R.drawable.towon)
                "03d", "03n" ->binding.icon.setImageResource(R.drawable.plaincouds)
                "04d", "04n" -> binding.icon.setImageResource(R.drawable.four)
                "09d", "09n" -> binding.icon.setImageResource(R.drawable.rainy)
                "10d", "10n" -> binding.icon.setImageResource(R.drawable.cloudy)
                "11d", "11n" -> binding.icon.setImageResource(R.drawable.thunder)
                "13d", "13n" -> binding.icon.setImageResource(R.drawable.snowflake)
                else -> binding.icon.setImageResource(R.drawable.sun) // A default icon
            }
        } else {
            showToast("No weather data available")
        }
    }


    fun translateWeatherDescription(description: String, context: Context): String {
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
            "broken clouds" -> context.getString(R.string.broken_clouds)
            "feelsLike" -> context.getString(R.string.feelsLike)
            "H" -> context.getString(R.string.H)
            "L" -> context.getString(R.string.L)


            else -> description // Return the description as-is if no translation exists
        }
    }

    private fun updateWeatherDetails(currentWeather: WeatherList) {
        with(binding) {
            pressureval.text = currentWeather.main.pressure.toString()
            humidityval.text = currentWeather.main.humidity.toString()
            windval.text = currentWeather.wind.speed.toString()
            visibilityval.text = currentWeather.visibility.toString()
            airqualityval.text = currentWeather.main.temp.toString()  // Placeholder
            uvval.text = currentWeather.main.sea_level.toString()  // Placeholder
            humiditygraphic.setImageResource(R.drawable.humidity)
            PressureGraphic.setImageResource(R.drawable.pressure)
            windGraphic.setImageResource(R.drawable.wind)
            visibilitygraphic.setImageResource(R.drawable.visibility)
            airqualitygraphic.setImageResource(R.drawable.airquality)
            uvgraphic.setImageResource(R.drawable.uv)
        }
    }

    private fun updateBackground(currentWeather: WeatherList) {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val isDayTime = hourOfDay in DAY_START_HOUR..DAY_END_HOUR
        val isClearSky = currentWeather.weather[0].description.contains("clear sky")

        binding.main.setBackgroundResource(
            if (isClearSky && isDayTime) R.drawable.clearskybackground
            else R.drawable.clearskynight
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.Main) {
            if (checkedPermissions()) {

                    handleInitialLocation()

                } else {
                    promptEnableLocationServices()

                }
            }
    }

    private fun checkedPermissions(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun promptEnableLocationServices() {
        showToast("Please enable location services")
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}

