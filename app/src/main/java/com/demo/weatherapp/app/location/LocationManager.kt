package com.demo.weatherapp.app.location

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient

const val GPS_PROVIDER_REQUEST_CHECK_SETTINGS = 113

class LocationManager(private val context: Context) {

    interface GpsProviderListener {
        fun isGpsProviderEnabled(isGPSEnabled: Boolean)
    }

    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private val locationSettingsRequest: LocationSettingsRequest?
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationClientLiveData.locationRequest)
        locationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
    }

    fun turnGPSOn(gpsProviderProviderListener: GpsProviderListener?) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsProviderProviderListener?.isGpsProviderEnabled(true)
        } else {
            settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(context as Activity) {
                    gpsProviderProviderListener?.isGpsProviderEnabled(true)
                }.addOnFailureListener {
                }.addOnFailureListener(context) { exception ->
                    when ((exception as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resApiException = exception as ResolvableApiException
                                resApiException.startResolutionForResult(
                                    context,
                                    GPS_PROVIDER_REQUEST_CHECK_SETTINGS
                                )
                            } catch (sendIntentException: Exception) {
                                sendIntentException.printStackTrace()
                                Log.d(
                                    javaClass.simpleName,
                                    "PendingIntent unable to execute request."
                                )
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage =
                                "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings."

                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }
}
