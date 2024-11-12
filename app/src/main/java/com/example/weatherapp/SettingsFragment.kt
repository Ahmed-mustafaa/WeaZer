package com.example.weatherapp

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.example.weatherapp.databinding.SettingsscreenBinding
import java.util.Locale

class SettingsFragment: Fragment() {


    private lateinit var binding: SettingsscreenBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // Inflate the layout for this fragment
        binding = SettingsscreenBinding.inflate(inflater, container, false)
        return binding.root

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val localeManager = requireContext().applicationContext.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
val currentLocale = sharedPreferences.getString("current_locale", "en") ?: "en"
        localeManager.applicationLocales = LocaleList(Locale(currentLocale))
// Set the desired locale; for example, to set Arabic as the language:
        binding.ArabicToggleButton.setOnClickListener {
            val newLocal = if(currentLocale == "en") "ar" else "en"
            sharedPreferences.edit().putString("current_locale", newLocal).apply()
            requireActivity().recreate()

        }
    }

}