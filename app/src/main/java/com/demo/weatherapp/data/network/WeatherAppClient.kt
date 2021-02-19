package com.demo.weatherapp.data.network

import android.content.Context
import com.demo.weatherapp.data.network.WeatherAppApi.Companion.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherAppClient(context: Context) {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(NoConnectionInterceptor(context))
                .build()
        ).build()

    val service: WeatherAppApi by lazy {
        retrofit.create(
            WeatherAppApi::class.java
        )
    }
}