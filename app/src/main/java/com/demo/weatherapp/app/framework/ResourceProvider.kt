package com.demo.weatherapp.app.framework

import android.content.Context
import androidx.annotation.StringRes

/**
 * ResourceProvider is a simple helper class for getting resources in non Android framework classes.
 */
class ResourceProvider(val context: Context): DefaultResourceProvider {

  override fun getResource(@StringRes resId: Int) = context.getString(resId)

  override fun getResource(@StringRes resId: Int, vararg format: Any) = context.getString(resId, *format)
}