package com.demo.weatherapp.data.network

import com.demo.weatherapp.data.model.WeatherDataDAO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAppApi {

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }

    @GET("weather/")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appId: String,
        @Query("units") units: String
    ): Response<WeatherDataDAO>
}