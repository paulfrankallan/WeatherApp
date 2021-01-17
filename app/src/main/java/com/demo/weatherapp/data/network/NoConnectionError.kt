package com.demo.weatherapp.data.network

import java.io.IOException

class NoConnectionError(errorMessage: String = "") : IOException(errorMessage)