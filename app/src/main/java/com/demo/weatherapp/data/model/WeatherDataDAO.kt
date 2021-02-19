package com.demo.weatherapp.data.model

import io.realm.RealmList
import io.realm.RealmObject

open class WeatherDataDAO(
    var weather: RealmList<WeatherDAO> = RealmList(),
    var main: MainDAO? = null,
    var wind: WindDAO? = null,
    var dt: Long? = null,
    var name: String? = null
): RealmObject() {

    fun toWeatherData() : WeatherData {
        return WeatherData(
            weather = weather.map { Weather(main = it.main, icon = it.icon) },
            main = Main(temp = main?.temp),
            wind = Wind(speed = wind?.speed, deg = wind?.deg),
            dt = dt,
            name = name,
        )
    }
}