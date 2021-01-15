package com.demo.weatherapp.data.repository

import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData

interface WeatherRepositoryApi {
    suspend fun syncWeather(weatherState: MutableLiveData<Result<WeatherData>>)
}