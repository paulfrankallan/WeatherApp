package com.demo.weatherapp.app.framework

import android.content.Context

/**
 * ResourceProvider is a simple helper class for getting resources in non Android framework classes.
 */
open class ResourceProvider(val context: Context) {

  open fun getResource(resId: Int): String {
    return context.getString(resId)
  }

  open fun getResource(resId: Int, vararg format: Any): String {
    return context.getString(resId, *format)
  }
}