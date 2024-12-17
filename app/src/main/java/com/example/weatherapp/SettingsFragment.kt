package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.weatherapp.databinding.SettingsscreenBinding
import java.util.Locale

class SettingsFragment: Fragment() {

    private val PREFS_NAME = "weather_prefs"
    private val TEMP_UNIT_KEY = "temp_unit"
    private val WIND_UNIT_KEY = "wind_unit"

    private lateinit var binding: SettingsscreenBinding
    private lateinit var sharedPreferences: SharedPreferences
    private  var isArabic:Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
/*
        newLocal = sharedPreferences.getString("current_locale", "en") ?: "en"
*/
        // Inflate the layout for this fragment
        binding = SettingsscreenBinding.inflate(inflater, container, false)
        return binding.root

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreLanguagePreference()
        binding.tempRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedTempUnit = when (checkedId) {
                R.id.celsiusRadioButton -> {
                    "Celsius".also {
                        binding.celsiusRadioButton.setBackgroundResource(android.R.color.darker_gray) // Set background for Celsius
                        binding.fahrenheitRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Fahrenheit
                        binding.kelvinRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Kelvin
                    }
                }

                R.id.fahrenheitRadioButton -> {
                    "Fahrenheit".also {
                        binding.celsiusRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Celsius
                        binding.fahrenheitRadioButton.setBackgroundResource(android.R.color.darker_gray)// Set background for Fahrenheit
                        binding.kelvinRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Kelvin
                    }
                }

                R.id.kelvinRadioButton -> {
                    "Kelvin".also {
                        binding.celsiusRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Celsius
                        binding.fahrenheitRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Fahrenheit
                        binding.kelvinRadioButton.setBackgroundResource(android.R.color.darker_gray) // Set background for Kelvin
                    }
                }

                else -> {
                    "Celsius".also { // Default
                        binding.celsiusRadioButton.setBackgroundResource(android.R.color.darker_gray) // Set background for Celsius (default)
                        binding.fahrenheitRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Fahrenheit
                        binding.kelvinRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Kelvin
                    }
                }
            }
            saveTemperatureUnit(selectedTempUnit)
            Log.i("selectedTempUnit", selectedTempUnit)

        }
        binding.windRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedWindUnit = when (checkedId) {
                R.id.KMRadioButton -> {
                    "KM/HR".also {
                        binding.KMRadioButton.setBackgroundResource(android.R.color.darker_gray)
                        binding.MPHRadioButton.setBackgroundResource(android.R.color.transparent)
                    }
                }

                R.id.MPHRadioButton -> {
                    "MPH".also {
                        binding.KMRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Celsius
                        binding.MPHRadioButton.setBackgroundResource(android.R.color.darker_gray) // Set background for Fahrenheit
                    }
                }

                else -> {
                    "KM/HR".also {
                        binding.KMRadioButton.setBackgroundResource(android.R.color.darker_gray) // Set background for Celsius (default)
                        binding.MPHRadioButton.setBackgroundResource(android.R.color.transparent) // Clear background for Fahrenheit
                    }
                }
            }
            saveWindUnit(selectedWindUnit)
            Log.i("selectedWindUnit", selectedWindUnit)
        }
         // Check Android version before using LocaleManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager =
                requireContext().applicationContext.getSystemService(LocaleManager::class.java)

            // Get current locale from SharedPreferences (or default to "en")
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            var currentLocale = sharedPreferences.getString("current_locale", "en") ?: "en"

            // Update application locales
            localeManager?.applicationLocales = LocaleList(Locale(currentLocale))

            binding.ArabicSwitchButton.setOnCheckedChangeListener { _, isChecked ->
                currentLocale = if (isChecked) "ar" else "en"

                with(sharedPreferences.edit()) {
                    putString("current_locale", currentLocale)
                    apply()
                }

                localeManager?.applicationLocales = LocaleList(Locale(currentLocale))
                val locale = Locale(currentLocale)
                Locale.setDefault(locale)
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        } else {
            // Fallback for older versions: manually update configuration
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
           var  currentLocale = sharedPreferences.getString("current_locale", "en") ?: "en"

            binding.ArabicSwitchButton.setOnCheckedChangeListener { _, isChecked ->
                currentLocale = if (isChecked) "ar" else "en"

                with(sharedPreferences.edit()) {
                    putString("current_locale", currentLocale)
                    apply()
                }

                val locale = Locale(currentLocale)
                Locale.setDefault(locale)
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        }



        binding.Save.setOnClickListener {
            val Intent = Intent(requireContext(), MainActivity::class.java)
               startActivity(Intent)
        }
    }

    private fun saveTemperatureUnit(unit: String) {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TEMP_UNIT_KEY, unit).apply()
    } private fun saveWindUnit(unit: String) {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(WIND_UNIT_KEY, unit).apply()
    }
    private fun SaveLanguage(languageCode: String) {
        val sharedPreferences = requireContext().getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Save the selected language and its state
        editor.putString("current_locale", languageCode)
        // Save if English is selected
        editor.putBoolean("isArabic", languageCode == "ar")
        editor.apply()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        requireActivity().baseContext.resources.updateConfiguration(config, requireActivity().baseContext.resources.displayMetrics)
    }

    private fun restoreLanguagePreference() {
        val sharedPreferences = requireContext().getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val isArabic = sharedPreferences.getBoolean("isArabic", false) // Default to English (false)

        binding.ArabicSwitchButton.isChecked = isArabic // Set switch state
        val savedLocale = if (isArabic) "ar" else "en"

        // Update app language on start
        val locale = Locale(savedLocale)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)


    }

}