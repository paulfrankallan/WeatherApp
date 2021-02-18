package com.demo.weatherapp.app

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import io.realm.Realm
import io.realm.RealmConfiguration

@Suppress("unused")
@HiltAndroidApp
class WeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()

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
}