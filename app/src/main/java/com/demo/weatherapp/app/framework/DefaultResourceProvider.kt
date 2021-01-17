package com.demo.weatherapp.app.framework

import android.content.Context
import androidx.annotation.StringRes

/**
 * DefaultResourceProvider is a simple helper class for getting resources in non Android framework classes.
 */
interface DefaultResourceProvider {

  fun getResource(@StringRes resId: Int): String

  fun getResource(@StringRes resId: Int, vararg format: Any): String
}