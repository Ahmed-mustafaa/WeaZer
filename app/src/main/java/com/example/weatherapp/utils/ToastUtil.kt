package com.example.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.R
import java.util.Locale

object ToastUtil {
    fun showCustomToast(context: Context, message: String) {
        val inflater = LayoutInflater.from(context)
        val layout =
            inflater.inflate(R.layout.custom_toast, null) // custom_toast is your custom XML layout
        // Set the message in the custom layout
        layout.findViewById<TextView>(R.id.toast_text).text = message

        // Initialize and configure the Toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM, 0, 100) // Adjust as needed
        toast.show()
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
     fun convertToCelsius(temp: Double): Double {
        return temp
    }

     fun convertToFahrenheit(temp: Double): Double {
        return (temp * 9 / 5) + 32
    }

     fun convertToKelvin(temp: Double): Double {
        return (temp - 32) * 5 / 9 + 273.15
    }

     fun convertToMPH(speed: Double): Double {
        return speed * 0.621371
    }
      fun formatNumberToArabic(number: Int): String {
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

    @SuppressLint("BatteryLife")
     fun requestBatteryOptimizationPermission(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }
}