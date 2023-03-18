package com.sarthakpranesh.simplyweather.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

// constant values
const val minimumDistanceInMeter = 500F;
const val minimumTimeInMilliSeconds: Long = 1000 * 60 * 1

@SuppressLint("MissingPermission")
class Location(private val context: Context) {
    private val shouldLogInfo = true
    private var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationCaptured: Location? = null
    private var gpsEnabled = false
    private var internetEnabled = false

    private fun logInfo(message: String) {
        if (shouldLogInfo) {
            Log.i("Location package", message)
        }
    }

    fun subscribeToUserLocation(): Boolean {
        gpsEnabled = false
        internetEnabled = false
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsEnabled = true
            logInfo("GPS provider present")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minimumTimeInMilliSeconds,
                minimumDistanceInMeter,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationCaptured = location
                    }
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            internetEnabled = true
            logInfo("Network provider present")
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minimumTimeInMilliSeconds,
                minimumDistanceInMeter,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationCaptured = location
                    }
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )
        }

        if (internetEnabled || gpsEnabled) {
            return true
        }

        return false
    }

    fun getUserLocation(): Location? {
        logInfo("GPS: $gpsEnabled, Internet: $internetEnabled")
        if (gpsEnabled) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                return it
            }
        }
        if (internetEnabled) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                if (it.hasAccuracy() && locationCaptured?.hasAccuracy() == true) {
                    return if (it.accuracy > locationCaptured!!.accuracy) {
                        it
                    } else {
                        locationCaptured
                    }
                }
                return it
            }
        }
        return null
    }
}