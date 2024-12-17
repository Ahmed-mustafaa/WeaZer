package com.example.weatherapp.adapter
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemFavoriteBinding
import com.example.weatherapp.model.ForCast
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

class FavAdapter(private val Cities: MutableList<ForCast>,  private val context: Context
                 , private val onCityClick: (ForCast) -> Unit, private val onDeleteClick: (ForCast) -> Unit,
):RecyclerView.Adapter<FavAdapter.FavViewHolder>() {
    private val TEMP_UNIT_KEY = "temp_unit"
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavAdapter.FavViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return FavViewHolder(binding)
    }


    override fun onBindViewHolder(holder: FavAdapter.FavViewHolder, position: Int) {
        holder.bind(Cities[position], position, holder)
        val weather = Cities[position]
        val cityName = weather.city.name

// Use Geocoder to get the full location based on city name
        val context: Context = holder.itemView.context // or any context you have access to
        val geocoder = Geocoder(context, Locale.getDefault())

// This will search for the city name and try to get a list of addresses matching it
        try {
            val addresses =
                geocoder.getFromLocationName(
                    cityName,
                    30
                ) // You can adjust the maxResults as needed
            val selectedAddress = addresses!!.find { address ->
                        address.featureName?.equals(cityName, ignoreCase = true) == true ||
                        address.locality?.equals(cityName, ignoreCase = true) == true ||
                        address.subAdminArea?.equals(cityName, ignoreCase = true) == true ||
                        address.adminArea?.equals(cityName, ignoreCase = true) == true ||
                        address.countryName?.equals(cityName, ignoreCase = true) == true
            }
            Log.i("AddressFromFAVADAPTER","${selectedAddress}")

            val location = if (addresses.isEmpty()) {
                val validAddress = addresses.find { address ->
                            address.featureName?.equals(cityName, ignoreCase = true) == true ||
                            address.locality?.equals(cityName, ignoreCase = true) == true ||
                            address.subAdminArea?.equals(cityName, ignoreCase = true) == true ||
                            address.adminArea?.equals(cityName, ignoreCase = true) == true ||
                            address.countryName?.equals(cityName, ignoreCase = true) == true
                }

                // If a valid address is found, extract meaningful details
                validAddress?.let { address ->
                    val city =    address.featureName ?:address.locality ?: address.subAdminArea ?: address.adminArea?: address.countryName?:"Unknown City"
                    val country = address.countryName ?: "Unknown Country"
                    "$city, $country"
                }
            } else {
                Log.i("Mafish yb2a gettin elsee ","${addresses[0]}")
                val selectedAddress = addresses[0]
                val city =    selectedAddress.featureName ?:selectedAddress.locality ?: selectedAddress.subAdminArea?: selectedAddress.adminArea  ?: selectedAddress.countryName
                val country = selectedAddress.countryName ?: "Unknown Country"
                // Combine these parts into a single string
               if (city.isNullOrEmpty()) country else "$city, $country"
            }

// Set the city name or full address in the TextView
        holder.cityNameTextView.text = location ?: cityName
        }catch (e: IOException){
            e.printStackTrace()
        }
        Log.i("weather",weather.weatherList[position].dt_txt.toString())
        val sharedPrefs = holder.itemView.context.getSharedPreferences("current_locale", Context.MODE_PRIVATE)
        val TempSharedUnit = holder.itemView.context.getSharedPreferences("weatherprefs", Context.MODE_PRIVATE)
        val selectedUnit = TempSharedUnit.getString(TEMP_UNIT_KEY, "Celsius")
        val currentLanguage = Locale.getDefault()
        Log.i("currentLanguage", currentLanguage.toString())
        val temp = weather.weatherList[0].main.temp.roundToInt()
        Log.i("temp", temp.toString())
        if (currentLanguage == Locale("ar")) {
            holder.DisplayTemp.text = when (selectedUnit) {
                // Kelvin to Celsius
                "Fahrenheit" -> formatNumberToArabic((temp * 9 / 5) + 32) // celisus  to Fahrenheit
                "Kelvin" -> formatNumberToArabic((temp - 32) * 5 / 9 + 273.15.toInt()) // Kelvin remains Kelvin
                else -> formatNumberToArabic(temp) // Default to Kelvin
            }
        } else {
            holder.DisplayTemp.text = when (selectedUnit) {
                "Fahrenheit" -> "${(temp * 9 / 5) + 32}°F" // celisus  to Fahrenheit
                "Kelvin" -> "${(temp - 32) * 5 / 9 + 273.15}°K" // Kelvin remains Kelvin
                else -> "$temp °C" // Default to Kelvin
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "isArabic") {
                val currentLanguage = sharedPrefs.getString("isArabic", "en")
                Log.i("currentLanguage", currentLanguage.toString())
                val temp = weather.weatherList[0].main.temp.roundToInt()
                Log.i("temp", temp.toString())
                if (currentLanguage=="ar") {
                    holder.DisplayTemp.text = when (selectedUnit) {
                        // Kelvin to Celsius
                        "Fahrenheit" -> formatNumberToArabic((temp * 9 / 5) + 32)  // celisus  to Fahrenheit
                        "Kelvin" -> "${formatNumberToArabic((temp - 32) * 5 / 9 + 273.15.toInt())}" // Kelvin remains Kelvin
                        else -> "${formatNumberToArabic(temp)}" // Default to Kelvin
                    }
                } else {
                    Log.i("LocalFromElseListening", currentLanguage.toString())

                    holder.DisplayTemp.text = when (selectedUnit) {
                        "Fahrenheit" -> "${(temp * 9 / 5) + 32}°F" // celisus  to Fahrenheit
                        "Kelvin" -> "${(temp - 32) * 5 / 9 + 273.15}°K" // Kelvin remains Kelvin
                        else -> "$temp °C" // Default to Kelvin

                    }
                }
            }
        }

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

    override fun getItemCount(): Int {
        return Cities.size
    }
    inner class FavViewHolder(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(location: ForCast?, position: Int,holder: FavViewHolder){

            DisplayTemp.text="${location?.weatherList?.get(position)?.main?.temp}" + holder.itemView.context.getString(R.string.C)
            location?.weatherList?.get(position)?.dt_txt?.let { Log.d("Timestamp", it) }
            val Timing =location?.weatherList?.get(position)?.dt_txt?.substring(11,13)?.toIntOrNull()
            Time.text =Timing?.let {
                val isPm=it >=12
                val normalHour=if(it%12==0) 12 else it %12
                val amPm= if(isPm) "PM" else "AM"
                "$normalHour:00 $amPm"
            }?:"N/A"

            itemView.setOnClickListener {
                if (location != null) {
                    onCityClick(location)
                }
            }

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
        val cityNameTextView: TextView =itemView.findViewById(R.id.CityName)
        val DisplayTemp :TextView=(itemView.findViewById(R.id.DisplayTemp))
        val Time:TextView = itemView.findViewById(R.id.Time)

    }
}