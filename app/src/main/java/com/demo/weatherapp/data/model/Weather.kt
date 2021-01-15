package com.demo.weatherapp.data.model

import io.realm.RealmObject

open class Weather (
    var id: String? = null,
    var main: String? = null,
    var description: String? = null,
    var icon: String? = null
): RealmObject()