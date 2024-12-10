package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR  )/* && hasInternetConnection()*/ -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true

            else -> false
        }
    }

    fun hasInternetConnection(): Boolean {
        return try {
            val url = URL("https://www.google.com") // A reliable host to test
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000 // Timeout in milliseconds
            connection.connect()
            connection.responseCode == 200 // HTTP OK
        } catch (e: Exception) {
            Log.i("NetworkUtils", "Internet connection available")

            false
        }
    }
}