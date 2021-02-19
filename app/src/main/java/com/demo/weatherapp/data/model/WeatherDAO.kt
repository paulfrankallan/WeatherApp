package com.demo.weatherapp.data.model

import io.realm.RealmObject

open class WeatherDAO (
    var main: String? = null,
    var icon: String? = null
): RealmObject()