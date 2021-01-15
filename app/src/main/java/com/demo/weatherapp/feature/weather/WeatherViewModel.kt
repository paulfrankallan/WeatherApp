package com.demo.weatherapp.feature.weather

import androidx.lifecycle.*
import com.demo.weatherapp.R
import com.demo.weatherapp.app.degreesToHeadingString
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.app.unixTimeStampToLocalDateTime
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.network.NoConnectionError
import com.demo.weatherapp.data.repository.WeatherRepository
import com.demo.weatherapp.feature.weather.WeatherState.Action
import com.demo.weatherapp.feature.weather.WeatherState.Event
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.math.roundToInt

class WeatherViewModel : ViewModel(), KoinComponent {

    val actions = MutableLiveData<Action>()
    private var weatherState = WeatherState()
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
            is Result.Refreshing -> weatherState.copy(refreshing = it.refreshing)
            is Result.Error -> handleError(it.exception, it.data)
        }
    }

    val weather = MediatorLiveData<WeatherState>().apply {
        addSource(actionDispatcher, {})
        addSource(reducer) {
            weatherState = it
            value = it
            weatherState.events.clear()
        }
    }

    private fun mapWeatherData(weatherData: WeatherData) = weatherState.copy(
        currentCondition = weatherData.weather?.get(0)?.main ?: "",
        temperature = mapTemperature(weatherData),
        windSpeed = mapWindSpeed(weatherData),
        windDirection = weatherData.wind?.deg.degreesToHeadingString(),
        icon = weatherData.weather?.get(0)?.icon ?: "",
        updated = weatherData.dt?.unixTimeStampToLocalDateTime()
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

    private fun handleError(exception: Exception, weatherData: WeatherData?): WeatherState {
        when (exception) {
            is NoConnectionError -> {
                weatherState.events.add(
                    Event.ShowSnackbar(resourceProvider.getResource(R.string.no_internet_connection))
                )
            }
            else -> {
                weatherState.events.add(
                    Event.ShowSnackbar(resourceProvider.getResource(R.string.something_went_wrong))
                )
            }
        }
        // If we have good (recent < 24hrs) data then return it.
        return weatherData?.let { mapWeatherData(it) } ?: weatherState
    }
}