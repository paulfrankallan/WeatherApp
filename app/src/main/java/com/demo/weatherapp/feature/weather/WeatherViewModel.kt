package com.demo.weatherapp.feature.weather

import androidx.lifecycle.*
import com.demo.weatherapp.R
import com.demo.weatherapp.app.degreesToHeadingString
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.app.unixTimeStampToLocalDateTime
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.repository.WeatherRepository
import com.demo.weatherapp.feature.weather.WeatherState.Action
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.math.roundToInt

class WeatherViewModel : ViewModel(), KoinComponent {

    val actions = MutableLiveData<Action>()

    private var weatherSate = WeatherState()
    private val weatherRepository: WeatherRepository by inject()
    private val resourceProvider: ResourceProvider by inject()
    private val repositoryObserver = MutableLiveData<Result<WeatherData>>()

    private val actionDispatcher = Transformations.switchMap(actions) { action ->
        dispatch(action)
    }

    private fun dispatch(
        action: Action
    ): LiveData<Result<WeatherData>> = liveData {
        when (action) {
            is Action.Refresh -> {
                weatherRepository.syncWeather(repositoryObserver)
            }
        }
    }

    private val reducer = Transformations.map(repositoryObserver) {
        when (it) {
            is Result.Success -> mapWeatherData(it.data)
            is Result.Refreshing -> weatherSate.copy(refreshing = it.refreshing)
            is Result.Error -> weatherSate.copy()
        }
    }

    val weather = MediatorLiveData<WeatherState>().apply {
        addSource(actionDispatcher, {})
        addSource(reducer) {
            weatherSate = it
            value = it
        }
    }

    private fun mapWeatherData(weatherData: WeatherData) = weatherSate.copy(
        currentCondition = weatherData.weather?.get(0)?.main ?: "",
        temperature = mapTemperature(weatherData),
        windSpeed = mapWindSpeed(weatherData),
        windDirection = weatherData.wind?.deg.degreesToHeadingString(),
        icon = weatherData.weather?.get(0)?.icon ?: "",
        updated = weatherData.dt.unixTimeStampToLocalDateTime()
    )

    private fun mapTemperature(weatherData: WeatherData): String {
        return weatherData.main?.temp?.roundToInt()?.toString()?.let {
            resourceProvider.getResource(R.string.formatted_temperature, it)
        } ?: ""
    }

    private fun mapWindSpeed(weatherData: WeatherData): String {
        return weatherData.wind?.speed?.roundToInt()?.toString()?.let {
            resourceProvider.getResource(R.string.formatted_wind_speed, it)
        } ?: ""
    }
}