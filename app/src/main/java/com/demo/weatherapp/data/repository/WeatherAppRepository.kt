package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.BuildConfig
import com.demo.weatherapp.app.utcTimeStampToLocalDateTime
import com.demo.weatherapp.app.wasLessThan24HrsAgo
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.network.WeatherAppApi
import io.realm.Realm
import io.realm.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

open class WeatherAppRepository @Inject constructor(
    var realm: Realm = Realm.getDefaultInstance(),
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    var weatherAppApi: WeatherAppApi
) : WeatherAppRepositoryApi {

    // region Sync weather

    override suspend fun syncWeather(
        repositoryObserver: MutableLiveData<Result<WeatherData>>,
        location: Location?
    ) {
        withContext(ioDispatcher) {

            // Show local data < 24 hours old right away while network call made
            getLocal()?.let { localData ->
                val lastUpdated = localData.dt?.utcTimeStampToLocalDateTime()
                lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                    if (dateGood) {
                        repositoryObserver.value = Result.Success(localData)
                    }
                }
            }

            // We have no location info so don't make the network call
            if (location == null) return@withContext

            when (val result = getRemoteWeatherData(repositoryObserver, location)) {
                is Result.Success -> {
                    deleteAll()
                    save(result.data)
                    repositoryObserver.value = result
                }
                is Result.Error -> {
                    // IF offline & local data < 24 hours old THEN return data, location & updated
                    // ELSE IF offline data doesn't exist or out-of-date THEN return error with no data
                    // Note pass through any error to be handled not just no network connection
                    getLocal()?.let { localData ->
                        val lastUpdated = localData.dt?.utcTimeStampToLocalDateTime()
                        lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                            if (dateGood) {
                                // Error but we have good data, return error and data
                                repositoryObserver.value = Result.Error(result.exception, localData)
                            } else {
                                // Data out-of-date, return error
                                repositoryObserver.value = Result.Error(result.exception)
                            }
                        } ?: run { // Can't resolve date, return error
                            repositoryObserver.value = Result.Error(result.exception)
                        }
                    } ?: run { // No weather data, return error
                        repositoryObserver.value = Result.Error(result.exception)
                    }
                }
            }
        }
    }

    // endregion

    // region Remote data source

    private suspend fun getRemoteWeatherData(
        repositoryObserver: MutableLiveData<Result<WeatherData>>,
        location: Location
    ): Result<WeatherData> {
//        withContext(ioDispatcher) {
            return try {
                repositoryObserver.postValue(Result.Refreshing(true))
                val result = weatherAppApi.getWeather(
                    lat = location.latitude,
                    lon = location.longitude,
                    appId = BuildConfig.WEATHER_API_KEY,
                    units = "metric"
                )
                if (result.isSuccessful) {
                    result.body()?.let {
                        repositoryObserver.postValue(Result.Refreshing(false))
                        Result.Success(it)
                    } ?: Result.Error(Exception())
                } else {
                    repositoryObserver.postValue(Result.Refreshing(false))
                    Result.Error(Exception())
                }
            } catch (exception: Exception) {
                repositoryObserver.postValue(Result.Refreshing(false))
                Result.Error(exception)
            }
        }

    // endregion

    // region Local data operations

    private fun save(obj: RealmObject) = realm.executeTransaction { it.insertOrUpdate(obj) }

    private fun getLocal() = realm.where(WeatherData::class.java).findFirst()

    private fun deleteAll() = realm.executeTransaction { it.deleteAll() }

    // endregion
}
