package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData

interface WeatherAppRepositoryApi {
    suspend fun syncWeather(weatherState: MutableLiveData<Result<WeatherData>>, location: Location?)
}