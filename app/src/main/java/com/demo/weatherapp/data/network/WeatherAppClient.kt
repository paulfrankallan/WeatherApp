package com.demo.weatherapp.data.network

import com.demo.weatherapp.data.network.WeatherAppApi.Companion.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

object WeatherApi {
    val service: WeatherAppApi by lazy {
        retrofit.create(
            WeatherAppApi::class.java
        )
    }
}