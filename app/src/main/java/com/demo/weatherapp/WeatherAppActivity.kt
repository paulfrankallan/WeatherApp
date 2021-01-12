package com.demo.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.demo.weatherapp.feature.weather.WeatherFragment

class WeatherAppActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.weather_app_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, WeatherFragment.newInstance())
                    .commitNow()
        }
    }
}