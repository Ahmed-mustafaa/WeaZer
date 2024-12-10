package com.example.weatherapp

import android.content.Context
import android.location.Geocoder
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat
import org.osmdroid.util.GeoPoint
import java.util.Locale

object ToastUtil {
    fun getCurrentLocation(context: Context, location: GeoPoint): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 10)

        val selectedAddress = addresses?.find { address ->
            address.locality?.equals(location) == true ||
                    address.subAdminArea?.equals(location) == true ||
                    address.adminArea?.equals(location) == true ||
                    address.countryName?.equals(location) == true
        }

        // Return the full address if found, otherwise return a default message
        return selectedAddress?.getAddressLine(0) ?: "Location not found"
    }

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

}