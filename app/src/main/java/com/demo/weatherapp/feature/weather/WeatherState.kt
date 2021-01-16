package com.demo.weatherapp.feature.weather

import android.location.Location

data class WeatherState(
    val location: String? = null,
    val currentCondition: String = "",
    val temperature: String = "",
    val windSpeed: String = "",
    val windDirection: String = "",
    val icon: String? = null,
    val refreshing: Boolean = false,
    val noData: Boolean = true,
    val updated: String? = null,
    var events: MutableList<Event> = mutableListOf(),
) {
    sealed class Action {
        class Refresh(val location: Location?) : Action()
    }
    sealed class Event {
        class ShowSnackbar(val message: String) : Event()
    }
}