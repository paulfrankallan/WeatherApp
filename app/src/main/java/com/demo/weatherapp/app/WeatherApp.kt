package com.demo.weatherapp.app

import android.app.Application
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.app.location.LocationClientLiveData
import com.demo.weatherapp.data.network.WeatherAppApi
import com.demo.weatherapp.data.network.WeatherAppClient
import com.demo.weatherapp.data.repository.WeatherAppRepository
import com.demo.weatherapp.feature.weather.WeatherViewModel
import com.jakewharton.threetenabp.AndroidThreeTen
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

@Suppress("unused")
class WeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // DI
        startKoin {
            androidContext(this@WeatherApp)
            modules(listOf(weatherModule))
        }

        initRealm()
        AndroidThreeTen.init(this)
    }

    // region Realm DB

    private fun initRealm() {
        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build()
        )
    }

    // endregion

    // region Companion

    companion object {
        val weatherModule = module {
            viewModel { WeatherViewModel() }
            factory { Realm.getDefaultInstance() }
            single { LocationClientLiveData(get()) }
            single { ResourceProvider(get()) }
            single { Dispatchers.IO }
            single { WeatherAppClient.service }
            single { WeatherAppRepository(get(), get(), get()) }
        }
    }

    // endregion
}