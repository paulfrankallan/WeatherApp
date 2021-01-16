package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.R
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.app.unixTimeStampToLocalDateTime
import com.demo.weatherapp.app.wasLessThan24HrsAgo
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.network.WeatherApi
import io.realm.Realm
import io.realm.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val realm: Realm = Realm.getDefaultInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val resourceProvider: ResourceProvider
) : WeatherRepositoryApi {

    override suspend fun syncWeather(
        weatherState: MutableLiveData<Result<WeatherData>>,
        location: Location?
    ) {

        // Show local data < 24 hours old right away while network call made
        getLocal()?.let { localData ->
            val lastUpdated = localData.dt?.unixTimeStampToLocalDateTime()
            lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                if (dateGood) {
                    weatherState.value = Result.Success(localData)
                }
            }
        }

        // We have no location info so don't make the network call
        if(location == null) return

        when (val result = getRemoteWeatherData(weatherState, location)) {
            is Result.Success -> {
                deleteAll()
                save(result.data)
                weatherState.value = result
            }
            is Result.Error -> {
                // IF offline & local data < 24 hours old THEN return data, location & updated
                // ELSE IF offline data doesn't exist or out-of-date THEN return error with no data
                // Note pass through any error to be handled not just no network connection
                getLocal()?.let { localData ->
                    val lastUpdated = localData.dt?.unixTimeStampToLocalDateTime()
                    lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                        if (dateGood) {
                            // Error but we have good data, return error and data
                            weatherState.value = Result.Error(result.exception, localData)
                        } else {
                            // Data out-of-date, return error
                            weatherState.value = Result.Error(result.exception)
                        }
                    } ?: run { // Can't resolve date, return error
                        weatherState.value = Result.Error(result.exception)
                    }
                } ?: run { // No weather data, return error
                    weatherState.value = Result.Error(result.exception)
                }
            }
        }
    }

    private suspend fun getRemoteWeatherData(
        weatherState: MutableLiveData<Result<WeatherData>>,
        location: Location
    ): Result<WeatherData> =
        withContext(ioDispatcher) {
            return@withContext try {
                weatherState.postValue(Result.Refreshing(true))
                val result = WeatherApi.service.getWeather(
                    lat = location.latitude,
                    lon = location.longitude,
                    appId = resourceProvider.getResource(R.string.openweathermap_api_key)
                )
                if (result.isSuccessful) {
                    result.body()?.let {
                        weatherState.postValue(Result.Refreshing(false))
                        Result.Success(it)
                    } ?: Result.Error(Exception())
                } else {
                    weatherState.postValue(Result.Refreshing(false))
                    Result.Error(Exception())
                }
            } catch (exception: Exception) {
                weatherState.postValue(Result.Refreshing(false))
                Result.Error(exception)
            }
        }

    private fun save(obj: RealmObject) = realm.executeTransaction { it.insertOrUpdate(obj) }

    private fun getLocal() = realm.where(WeatherData::class.java).findFirst()

    private fun deleteAll() = realm.executeTransaction { it.deleteAll() }
}
