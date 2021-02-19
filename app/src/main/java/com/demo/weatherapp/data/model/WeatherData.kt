package com.demo.weatherapp.data.model

data class WeatherData(
    var weather: Collection<Weather>? = null,
    var main: Main? = null,
    var wind: Wind? = null,
    var dt: Long? = null,
    var name: String? = null
)