package com.demo.weatherapp.app.framework

import android.content.Context

class ResourceProvider(val context: Context) {

  fun getResource(resId: Int): String {
    return context.getString(resId)
  }

  fun getResource(resId: Int, vararg format: Any): String {
    return context.getString(resId, *format)
  }
}