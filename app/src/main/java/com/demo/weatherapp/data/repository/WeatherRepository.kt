package com.demo.weatherapp.data.repository

import androidx.lifecycle.MutableLiveData
import com.demo.weatherapp.app.unixTimeStampToLocalDateTime
import com.demo.weatherapp.app.wasLessThan24HrsAgo
import com.demo.weatherapp.app.wasMoreThan24HrsAgo
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepositoryApi {

    override suspend fun syncWeather(weatherState: MutableLiveData<Result<WeatherData>>) {

        val localData = getLocal()

        val lastUpdated = localData?.dt?.unixTimeStampToLocalDateTime()

        lastUpdated?.wasLessThan24HrsAgo()?.let {
            if(it) weatherState.value = Result.Success(localData)
        }

        lastUpdated?.wasMoreThan24HrsAgo()?.let {
            // TODO
        }

        val networkData = syncWeatherData(weatherState)

        if (networkData is Result.Success) {
            deleteAll()
            save(networkData.data)
        }

        weatherState.value = networkData
    }

    private suspend fun syncWeatherData(weatherState: MutableLiveData<Result<WeatherData>>): Result<WeatherData> =
        withContext(ioDispatcher) {
            return@withContext try {
                weatherState.postValue(Result.Refreshing(true))
                val result = WeatherApi.service.getByCity(
                    "Belfast",
                    "f45f1176cabb468b3b078caeb8ef955c"
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

    val isEmpty = realm.isEmpty
}
