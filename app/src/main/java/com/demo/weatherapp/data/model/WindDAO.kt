package com.demo.weatherapp.data.model

import io.realm.RealmObject

open class WindDAO(
    var speed: Float? = null,
    var deg: Float? = null
): RealmObject()