package com.demo.weatherapp.app.framework

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.PluralsRes
import com.demo.weatherapp.app.isNetworkConnected

class ResourceProvider(val context: Context) {

  fun getResource(resId: Int): String {
    return context.getString(resId)
  }

  fun getResource(resId: Int, vararg format: Any): String {
    return context.getString(resId, *format)
  }

  fun getQuantityString(@PluralsRes id: Int, quantity: Int, vararg format: Any): String {
    return context.resources.getQuantityString(id, quantity, *format)
  }

  fun hasPermissions(permissions: Array<String>): Boolean {
    return permissions.all { permission ->
      context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun isNetworkConnected(): Boolean = context.isNetworkConnected()
}