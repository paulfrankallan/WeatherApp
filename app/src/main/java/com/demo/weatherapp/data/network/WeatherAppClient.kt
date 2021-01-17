package com.demo.weatherapp.data.network

import com.demo.weatherapp.data.network.WeatherAppApi.Companion.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherAppClient {
    val service: WeatherAppApi by lazy {
        retrofit.create(
            WeatherAppApi::class.java
        )
    }
}

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(
        OkHttpClient.Builder()
            .addInterceptor(NoConnectionInterceptor())
            .build()
    ).build()