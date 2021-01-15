package com.demo.weatherapp.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.weatherapp.R
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