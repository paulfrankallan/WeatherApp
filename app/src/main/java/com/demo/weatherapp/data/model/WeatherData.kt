package com.demo.weatherapp.data.model

import io.realm.RealmList
import io.realm.RealmObject

open class WeatherData(
    var weather: RealmList<Weather>? = null,
    var main: Main? = null,
    var wind: Wind? = null,
    var dt: Long? = null
): RealmObject()