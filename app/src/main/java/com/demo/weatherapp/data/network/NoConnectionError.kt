package com.demo.weatherapp.data.network

import java.io.IOException

data class NoConnectionError(private val errorMessage: String = "") : IOException(errorMessage)