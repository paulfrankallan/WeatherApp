package com.demo.weatherapp.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.weatherapp.R

class WeatherAppActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.weather_app_activity)
    }
}