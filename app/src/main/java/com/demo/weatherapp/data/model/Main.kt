package com.demo.weatherapp.data.model

import io.realm.RealmObject

open class Main(
    var temp: Float? = null
): RealmObject()