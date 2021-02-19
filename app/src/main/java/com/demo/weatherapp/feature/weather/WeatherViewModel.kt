package com.demo.weatherapp.feature.weather

import android.location.Location
import androidx.lifecycle.*
import com.demo.weatherapp.R
import com.demo.weatherapp.app.degreesToHeadingString
import com.demo.weatherapp.app.framework.DefaultResourceProvider
import com.demo.weatherapp.app.location.LocationClientLiveData
import com.demo.weatherapp.app.utcTimeStampToLocalDateTime
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.network.NoConnectionError
import com.demo.weatherapp.data.repository.WeatherAppRepository
import com.demo.weatherapp.feature.weather.WeatherState.Action
import com.demo.weatherapp.feature.weather.WeatherState.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class WeatherViewModel @Inject constructor(): ViewModel() {

    // region Members

    @Inject lateinit var location: LocationClientLiveData
    @Inject lateinit var weatherRepository: WeatherAppRepository
    @Inject lateinit var resourceProvider: DefaultResourceProvider
    private val actions = MutableLiveData<Action>()
    private val repositoryObserver = MutableLiveData<Result<WeatherData>>()

    // endregion

    fun syncWeather(location: Location?) {
        actions.value = Action.Refresh(location)
    }

    // region Dispatch & Reduce

    private val actionDispatcher = Transformations.switchMap(actions) { action ->
        dispatch(action)
    }

    private fun dispatch(
        action: Action
    ): LiveData<Result<WeatherData>> = liveData {
        when (action) {
            is Action.Refresh -> {
                weatherRepository.syncWeather(repositoryObserver, action.location)
            }
        }
    }

    private val reducer: LiveData<WeatherState> = Transformations.map(repositoryObserver) {
        when (it) {
            is Result.Success -> mapWeatherData(it.data)
            is Result.Refreshing -> weather.value?.copy(refreshing = it.refreshing)
            is Result.Error -> handleError(it.exception, it.data)
        }
    }

    // endregion

    // region Mediator

    val weather = MediatorLiveData<WeatherState>().apply {
        value = WeatherState(refreshing = true) // Initialise state
        addSource(actionDispatcher) {} // Handle actions
        addSource(reducer) { // Handle state
            value = it
            it.events.clear()
        }
    }

    // endregion

    // region Map & format data

    private fun mapWeatherData(
        weatherData: WeatherData?, events: MutableList<Event> = mutableListOf()
    ): WeatherState {
        val weather = weatherData?.let {
            weather.value?.copy(
                location = formatLocation(weatherData),
                currentCondition = weatherData.weather?.elementAt(0)?.main ?: "",
                temperature = formatTemperature(weatherData),
                windSpeed = formatWindSpeed(weatherData),
                windDirection = weatherData.wind?.deg.degreesToHeadingString(),
                icon = weatherData.weather?.elementAt(0)?.icon ?: "",
                updated = formatUpdated(weatherData),
                noData = false
            )
        } ?: WeatherState(noData = true)
        weather.events.addAll(events)
        return weather
    }

    private fun formatTemperature(weatherData: WeatherData) =
        weatherData.main?.temp?.roundToInt()?.toString()?.let {
            resourceProvider.getResource(R.string.formatted_temperature, it)
        } ?: ""

    private fun formatWindSpeed(weatherData: WeatherData) =
        weatherData.wind?.speed?.roundToInt()?.toString()?.let {
            resourceProvider.getResource(R.string.formatted_wind_speed, it)
        } ?: ""

    private fun formatLocation(weatherData: WeatherData) =
        weatherData.name?.let {
            resourceProvider.getResource(R.string.location, it)
        }

    private fun formatUpdated(weatherData: WeatherData) =
        weatherData.dt?.utcTimeStampToLocalDateTime()
            ?.atZone(ZoneId.systemDefault())?.let {
                return resourceProvider.getResource(
                    R.string.last_updated,
                    it.format(
                        DateTimeFormatter.ofLocalizedDateTime(
                            FormatStyle.MEDIUM, FormatStyle.SHORT
                        )
                    )
                )
            } ?: ""

    // endregion

    // region Error handling

    private fun handleError(exception: Exception, weatherData: WeatherData?): WeatherState {
        // Return Error info and If we have good data (recent <= 24hrs) then return it also.
        return mapWeatherData(
            weatherData,
            arrayListOf(
                when (exception) {
                    is NoConnectionError -> {
                        Event.ShowSnackbar(resourceProvider.getResource(R.string.no_internet_connection))
                    }
                    else -> {
                        Event.ShowSnackbar(resourceProvider.getResource(R.string.something_went_wrong))
                    }
                }
            )
        )
    }

    // endregion
}