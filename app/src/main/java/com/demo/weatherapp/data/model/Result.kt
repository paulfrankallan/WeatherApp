package com.demo.weatherapp.data.model

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data class Refreshing(val refreshing: Boolean) : Result<Nothing>()
}