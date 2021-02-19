package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.BuildConfig
import com.demo.weatherapp.app.framework.RealmFactory
import com.demo.weatherapp.app.utcTimeStampToLocalDateTime
import com.demo.weatherapp.app.wasLessThan24HrsAgo
import com.demo.weatherapp.data.model.*
import com.demo.weatherapp.data.network.WeatherAppApi
import io.realm.Realm
import io.realm.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

open class WeatherAppRepository @Inject constructor(
    var realmFactory: RealmFactory,
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    var weatherAppApi: WeatherAppApi
) : WeatherAppRepositoryApi {

    // region Sync weather

    override suspend fun syncWeather(
        repositoryObserver: MutableLiveData<Result<WeatherData>>,
        location: Location?
    ) {
        withContext(ioDispatcher) {

            realmFactory.getRealm().use { realm ->

                // Show local data < 24 hours old right away while network call made
                realm.getLocal()?.toWeatherData()?.let { localData ->
                    val lastUpdated = localData.dt?.utcTimeStampToLocalDateTime()
                    lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                        if (dateGood) {
                            repositoryObserver.postValue(Result.Success(localData))
                        }
                    }
                }

                // We have no location info so don't make the network call
                if (location == null) return@withContext

                when (val result = getRemoteWeatherData(repositoryObserver, location)) {
                    is Result.Success -> {

                        realm.executeTransaction{ it.deleteAll() }

                        val temp = WeatherDataDAO(
                            name = result.data.name,
                            wind = WindDAO(speed = result.data.wind?.speed, deg = result.data.wind?.deg),
                            main = MainDAO(temp = result.data.main?.temp),
                            dt = result.data.dt,
                        )

                        result.data.weather?.let { temp.weather.addAll(it.map {
                            WeatherDAO(main = it.main, icon = it.icon)
                        }) }

                        realm.save(temp)

                        repositoryObserver.postValue(result)
                    }
                    is Result.Error -> {
                        // IF offline & local data < 24 hours old THEN return data, location & updated
                        // ELSE IF offline data doesn't exist or out-of-date THEN return error with no data
                        // Note pass through any error to be handled not just no network connection
                        realm.getLocal()?.let { localData ->
                            val lastUpdated = localData.dt?.utcTimeStampToLocalDateTime()
                            lastUpdated?.wasLessThan24HrsAgo()?.let { dateGood ->
                                if (dateGood) {
                                    // Error but we have good data, return error and data
                                    repositoryObserver.postValue(
                                        Result.Error(
                                            result.exception,
                                            localData.toWeatherData()
                                        )
                                    )
                                } else {
                                    // Data out-of-date, return error
                                    repositoryObserver.postValue(Result.Error(result.exception))
                                }
                            } ?: run { // Can't resolve date, return error
                                repositoryObserver.postValue(Result.Error(result.exception))
                            }
                        } ?: run { // No weather data, return error
                            repositoryObserver.postValue(Result.Error(result.exception))
                        }
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
                        Result.Success(it.toWeatherData())
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

    private fun Realm.save(obj: RealmObject) = executeTransaction { it.insertOrUpdate(obj) }

    private fun Realm.getLocal() = where(WeatherDataDAO::class.java).findFirst()

    // endregion
}
