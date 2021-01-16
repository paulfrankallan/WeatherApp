package com.demo.weatherapp.app

import android.app.Application
import android.content.ContextWrapper
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.data.repository.WeatherRepository
import com.demo.weatherapp.feature.weather.WeatherViewModel
import com.jakewharton.threetenabp.AndroidThreeTen
import com.karumi.dexter.Dexter
import com.pixplicity.easyprefs.library.Prefs
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

@Suppress("unused")
class WeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        initPrefs()
        initRealm()

        startKoin {
            androidContext(this@WeatherApp)
            modules(listOf(weatherModule))
        }
    }

    // region Realm

    private fun initRealm() {
        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build()
        )
    }

    // endregion

    // region Prefs

    private fun initPrefs() {
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
    }

    // endregion

    // region Companion

    companion object {
        val weatherModule = module {
            viewModel { WeatherViewModel() }
            factory { Realm.getDefaultInstance() }
            single { ResourceProvider(get()) }
            single { Dispatchers.IO }
            single { WeatherRepository(get(), get(), get()) }
        }
    }

    // endregion
}