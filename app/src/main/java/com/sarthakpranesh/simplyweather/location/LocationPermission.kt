package com.sarthakpranesh.simplyweather.location

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.sarthakpranesh.simplyweather.MainActivity

class LocationPermission(
    private var activity: MainActivity
    ) {

    fun isLocationPermissionGranted(): Boolean {
        return (
                    !(ActivityCompat.checkSelfPermission(
                        activity.applicationContext,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED)
                )
    }

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1009
        )
    }
}