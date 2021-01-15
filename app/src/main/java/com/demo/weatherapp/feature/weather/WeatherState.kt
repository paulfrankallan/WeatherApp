package com.demo.weatherapp.feature.weather

import org.threeten.bp.LocalDateTime

data class WeatherState(
    val currentCondition: String = "",
    val temperature: String = "",
    val windSpeed: String = "",
    val windDirection: String = "",
    val icon: String? = null,
    val refreshing: Boolean = false,
    val updated: LocalDateTime? = null
) {
    sealed class Action {
        object Refresh : Action()
    }
}