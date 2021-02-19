package com.demo.weatherapp.data.model

import io.realm.RealmObject

open class MainDAO(
    var temp: Float? = null
): RealmObject()