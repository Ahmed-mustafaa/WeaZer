package com.example.weatherapp.service

import com.example.weatherapp.Utils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {
    companion object {


        private val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout
            .readTimeout(60, TimeUnit.SECONDS)     // Read timeout
            .writeTimeout(30, TimeUnit.SECONDS)    // Write timeout
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()



        val apiService: Service by lazy {
            retrofit.create(Service::class.java)
        }

    }
}

  /*  companion object{
        val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Utils.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON conversion
                .build()
        }
        }*/

