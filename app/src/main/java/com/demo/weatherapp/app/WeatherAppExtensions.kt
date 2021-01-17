package com.demo.weatherapp.app

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import java.util.*
import kotlin.math.roundToInt

fun LocalDateTime.wasLessThan24HrsAgo() =
    this.isAfter(LocalDateTime.now().minusHours(24))

fun Long.unixTimeStampToLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(Date(this * 1000).time)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Float?.degreesToHeadingString(): String {
    return this?.let {
        arrayOf(
            "North", "North East", "East",
            "South East", "South", "South West",
            "West", "North West", "North"
        )[(this % 360 / 45).roundToInt()]
    } ?: ""
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(
        lifecycleOwner,
        object : Observer<T> {
            override fun onChanged(t: T?) {
                observer.onChanged(t)
                removeObserver(this)
            }
        }
    )
}
