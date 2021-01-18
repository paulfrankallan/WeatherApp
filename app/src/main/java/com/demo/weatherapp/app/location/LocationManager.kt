package com.demo.weatherapp.app.location

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.widget.Toast
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient

const val GPS_PROVIDER_REQUEST_CHECK_SETTINGS = 113

class LocationManager(private val context: Context) {

    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest
        .Builder()
        .addLocationRequest(LocationClientLiveData.locationRequest)
        .setAlwaysShow(true).build()

    fun turnOnGps(listener: ((isGpsProviderEnabled: Boolean) -> Unit)) {
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            listener.invoke(true)
        } else {
            settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(context as Activity) {
                    listener.invoke(true)
                }.addOnFailureListener(context) { exception ->
                    when ((exception as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            attemptGpsIssueResolution(context, exception)
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location services problem. Fix in Settings."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun attemptGpsIssueResolution(context: Activity, exception: Exception) {
        try {
            (exception as ApiException).status.resolution?.intentSender?.let {
                startIntentSenderForResult(
                    context, it, GPS_PROVIDER_REQUEST_CHECK_SETTINGS,
                    null, 0, 0, 0, null
                )
            }
        } catch (sendIntentException: Exception) {
            sendIntentException.printStackTrace()
            val errorMessage = "Location services problem. Fix in Settings."
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
