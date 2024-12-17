import android.content.Context
import com.example.weatherapp.model.FavoriteLocation
import com.example.weatherapp.model.ForCast
import com.google.gson.Gson

class SharedPrefs  constructor(context: Context) {
    private val preferences = context.getSharedPreferences("myprefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    companion object {
        private const val SHARED_PREFS_NAME = "myprefs"
        private const val AlarmsSHARED_PREFS_NAME = "alarms_prefs"
        private val ALARM_SET_KEY = "alarmSet"

        private const val KEY_CITIES = "favoriteWeatherList"
        private const val KEY_Alarms = "Alarm"
        private const val KEY_ALARM = "isAlarmAdded"

        private var instance: SharedPrefs? = null

        fun getInstance(context: Context): SharedPrefs {
            if (instance == null) {
                instance = SharedPrefs(context.applicationContext)
            }
            return instance!!
        }
    }
    fun getCachedWeather(): ForCast? {
        val weatherJson = preferences.getString("cached_weather", null)
        return if (weatherJson != null) {
            gson.fromJson(weatherJson, ForCast::class.java)
        } else {
            null
        }
    }
    fun clearCachedWeather() {
        preferences.edit().remove("cached_weather").apply()
    }
    fun isAlarmAdded(): Boolean {
        return preferences.getBoolean(KEY_ALARM, false)
    }
    fun setAlarmAdded(value: Boolean) {
        preferences.edit().putBoolean(KEY_ALARM, value).apply()

    }
    fun saveWeather(weather: ForCast) {
        val weatherJson = Gson().toJson(weather)
        preferences.edit().putString("cached_weather", weatherJson).apply()
    }

    // Add a removeCity function to delete a specific city from favorites
    fun removeCity(cityName: String) {
        val favoriteCitiesJson = preferences.getString(KEY_CITIES, null)
        if (!favoriteCitiesJson.isNullOrEmpty()) {
            val favoriteCities = Gson().fromJson(favoriteCitiesJson, Array<FavoriteLocation>::class.java).toMutableList()
            favoriteCities.removeAll { it.cityName == cityName }

            preferences.edit().putString(KEY_CITIES, Gson().toJson(favoriteCities)).apply()

        }
    }
   /* fun removeAlarm() {
        val AlarmJson = preferences.getString(KEY_Alarms, null)
        if (!AlarmJson.isNullOrEmpty()) {
            val Alarmms = Gson().fromJson(AlarmJson, Array<AlarmScreen.Alarm>::class.java).toMutableList()
            Alarmms.removeAll { it. == cityName }

            preferences.edit().putString(KEY_CITIES, Gson().toJson(AlarmJson)).apply()

        }
    }*/
}
