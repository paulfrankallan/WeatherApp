package com.demo.weatherapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class NoConnectionInterceptor : Interceptor {

  @Inject lateinit var  context: Context

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    if (!connected) {
      throw NoConnectionError()
    }
    return chain.proceed(chain.request())
  }

  private val connected: Boolean
    get() { // TODO - Update deprecated.
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = cm.activeNetworkInfo
      return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}