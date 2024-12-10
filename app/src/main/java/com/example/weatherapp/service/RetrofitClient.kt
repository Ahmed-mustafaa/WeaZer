package com.example.weatherapp.service

import com.example.weatherapp.Utils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
class RetrofitClient {
    companion object {


        private val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val BASE_URL_TRANSLATE = "https://libretranslate.com/"

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

        private val translateRetrofit = Retrofit.Builder()
            .baseUrl(BASE_URL_TRANSLATE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val translateApiService: LibreTranslateApi by lazy {
            translateRetrofit.create(LibreTranslateApi::class.java)
        }

        val apiService: Service by lazy {
            retrofit.create(Service::class.java)
        }

    }
    fun HandleHttpRequest(url: String): Exception {
        return when (url) {
            "400" -> HttpException("Bad Request: The server could not understand the request.")
            "401" -> HttpException("Unauthorized: Authentication is required.")
            "403" -> HttpException("Forbidden: Access to the resource is denied.")
            "404" -> HttpException("Not Found: The requested resource could not be found.")
            "500" -> HttpException("Internal Server Error: The server encountered an error.")
            else -> Exception("Unhandled error code or invalid URL")
        }
    }
}
class HttpException(message: String) : Exception(message)


  /*  companion object{
        val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Utils.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON conversion
                .build()
        }
        }*/

