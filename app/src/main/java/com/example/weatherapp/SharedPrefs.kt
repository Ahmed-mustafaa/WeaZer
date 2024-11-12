package com.example.weatherapp

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.example.weatherapp.model.ForCast
import com.google.gson.Gson

class SharedPrefs private constructor(context: Context){
    private val preferences = context.getSharedPreferences("myprefs", Context.MODE_PRIVATE)

    companion object{

        private const val SHARED_PREFS_NAME= "myprefs"
        private const val KEY_CITIES = "cities"
        private var instance: SharedPrefs? = null

        fun getInstance(context: Context): SharedPrefs {
            if (instance == null) {
                instance = SharedPrefs(context.applicationContext)
            }
            return instance!!
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Store a list of favorite cities
    fun setCities(cities: Set<String>) {
        prefs.edit().putStringSet(KEY_CITIES, cities).apply()
    }
    fun addCity(city: String,lat:String,lon:String) {
        val editor = preferences.edit()
        val cities = preferences.getStringSet("favorite_cities", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val cityData = "$city,$lat,$lon"
        cities.add(cityData)
        cities.add(city) // Add city to the set
        editor.putStringSet("favorite_cities", cities) // Save updated set
        editor.apply() // Commit changes
    }



    // Retrieve the list of favorite cities
    fun getCities(): MutableSet<String>? {
        return prefs.getStringSet(KEY_CITIES, null)
    }

    // Clear all shared preferences
    fun clearPrefs() {
        prefs.edit().clear().apply()
    }
}

