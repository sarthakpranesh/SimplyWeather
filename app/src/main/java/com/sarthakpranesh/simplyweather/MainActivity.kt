package com.sarthakpranesh.simplyweather

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.airbnb.lottie.LottieDrawable
import com.sarthakpranesh.simplyweather.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask


data class WeatherDetails ( var temperature: String, var wind: String, var condition: String, var city: String, var loading: Boolean = true)

class MainActivity : Activity() {
    lateinit var binding: ActivityMainBinding
    private val lottieAnimationsMap = mapOf(
        "clearDay" to R.raw.clear_day,
        "clearNight" to R.raw.clear_night,
        "cloudDay" to R.raw.cloud_day,
        "cloudNight" to R.raw.cloud_night,
        "rainDay" to R.raw.rain_day,
        "rainNight" to R.raw.rain_night,
        "snowDay" to R.raw.snow_day,
        "snowNight" to R.raw.snow_night,
        "storm" to R.raw.storm,
        "thunder" to R.raw.thunder,
        "windy" to R.raw.windy,
        "mist"  to R.raw.mist,
    )
    private var defaultCurAnimation = 0
    private var weather = WeatherDetails("Loading", "", "", "")
    private var animationTimer: Timer = Timer()
    private var animationTimerStarted = false
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onResume() {
        super.onResume()

        if (!isLocationPermissionGranted()) {
            binding.condition.text = "Please provide location permission to use the app!"
            binding.temperature.text = "No Permission"
            setLottieAnimation("random")
            return
        } else {
            getUserLocation()
        }

        if (weather.loading) {
            binding.condition.text = "Looking for clouds and rain around you!"
            binding.temperature.text = "Loading"
            setLottieAnimation("random")
        }

        CoroutineScope(Dispatchers.IO).launch {
            // fetch and update weather details in data class
            val wasSuccess = getWeatherDetails()
            Log.i("Weather", weather.toString())
            // update ui
            if (wasSuccess) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.temperature.text = weather.temperature
                    binding.condition.text = weather.condition + " in " + weather.city
                    understandAndSetLottie()
                }
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1009
            )
            false
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i("Weather", "GPS provider present")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                10F,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.i("Weather", location.latitude.toString() + "-" + location.longitude.toString())
                    }
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {
                        Log.i("Weather", "Provider Enabled")
                    }
                    override fun onProviderDisabled(provider: String) {
                        Log.i("Weather", "Provider Disabled")
                    }
                }
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.i("Weather", "Network provider present")
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,
                10F,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.i("Weather", location.latitude.toString() + "-" + location.longitude.toString())
                    }
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {
                    }
                    override fun onProviderDisabled(provider: String) {
                    }
                }
            )
        }
    }

    private fun allErrorHandler(
        heading: String = "Error",
        message: String = "We are working towards supporting your location soon!"
    ) {
        runOnUiThread {
            binding.condition.text = message
            binding.temperature.text = heading
        }
    }

    // get weather condition, https://wttr.in/Delhi?format=j1
    @SuppressLint("MissingPermission")
    private fun getWeatherDetails(recurringForLocation: Boolean = false): Boolean {
        val clint = OkHttpClient()
        try {
            val locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // select best accuracy location
            var location = if (locationGps !== null) locationGps else locationNetwork
            if (locationGps != null && locationNetwork != null) {
                location = if (locationGps.accuracy < locationNetwork.accuracy) locationNetwork else locationGps
            }

            // enforce location retrieval
            if (location == null) {
                if (recurringForLocation) {
                    Log.i("Weather", "Failed")
                    allErrorHandler(message = "Unable to retrieve device location! Please provide location access.")
                    return false
                }
                Thread.sleep(1000 * 3)
                return getWeatherDetails(true)
            }

            Log.i("lat-long", location.latitude.toString() + "-" + location.longitude.toString())
            val addresses = Geocoder(applicationContext).getFromLocation(location.latitude, location.longitude, 1)
            if (addresses.size == 0) {
                allErrorHandler()
                return false
            }

            Log.i("Weather", addresses[0].locality)
            val response = clint.newCall(Request.Builder().url("https://weather.sarthak.work/api/weather?city=${addresses[0].locality}").build()).execute()
            if (response.body != null) {
                val jsonObject = JSONObject(response.body!!.string())
                weather.temperature = jsonObject.get("temperature") as String
                weather.wind = jsonObject.get("wind") as String
                weather.condition = jsonObject.get("description") as String
                weather.city = addresses[0].locality
                weather.loading = false
            }
            response.close()
            return true
        } catch (e: IOException) {
            // do something on exception
            Log.i("Error", e.message.toString())
            return false
        }
    }

    private fun understandAndSetLottie() {
        // check for storm or thunderstorm
        if (doesAnyKeyExistInWeatherCondition(arrayOf("thunderstorm", "storm"))) {
            setLottieAnimation("storm")
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // check for thunder
        if (doesAnyKeyExistInWeatherCondition(arrayOf("thunder"))) {
            setLottieAnimation("thunder")
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // check for windy
        if (doesAnyKeyExistInWeatherCondition(arrayOf("windy"))) {
            setLottieAnimation("windy")
            setUiColorScheme(R.color.windy_bg, R.color.windy_text)
            return
        }

        // check for mist
        if (doesAnyKeyExistInWeatherCondition(arrayOf("mist", "haze"))) {
            setLottieAnimation("mist")
            setUiColorScheme(R.color.cloudy_bg, R.color.cloudy_text)
            return
        }

        // check for clear
        if (doesAnyKeyExistInWeatherCondition(arrayOf("clear", "sunny"))) {
            // set according to day time
            if (checkIfIsNight()) {
                setLottieAnimation("clearNight")
            } else {
                setLottieAnimation("clearDay")
            }
            setUiColorScheme(R.color.sunny_bg, R.color.sunny_text)
            return
        }

        // check for clouds
        if (doesAnyKeyExistInWeatherCondition(arrayOf("cloudy", "cloud", "overcast"))) {
            // set according to day time
            if (checkIfIsNight()) {
                setLottieAnimation("cloudNight")
            } else {
                setLottieAnimation("cloudDay")
            }
            setUiColorScheme(R.color.cloudy_bg, R.color.cloudy_text)
            return
        }

        // check for rain
        if (doesAnyKeyExistInWeatherCondition(arrayOf("rainy", "rain"))) {
            // set according to day time
            if (checkIfIsNight()) {
                setLottieAnimation("rainNight")
            } else {
                setLottieAnimation("rainDay")
            }
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // check for snow
        if (doesAnyKeyExistInWeatherCondition(arrayOf("snow"))) {
            // set according to day time
            if (checkIfIsNight()) {
                setLottieAnimation("snowNight")
            } else {
                setLottieAnimation("snowDay")
            }
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // if non satisfy, put weather animations on loop
        setLottieAnimation("random")
    }

    private fun checkIfIsNight(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal[Calendar.HOUR_OF_DAY]
        return hour < 6 || hour > 18
    }

    private fun doesAnyKeyExistInWeatherCondition(keys: Array<String>): Boolean {
        keys.forEach {
            if (weather.condition.lowercase().contains(it, true)) {
                return true
            }
        }
        return false
    }

    private fun setLottieAnimation(animationName: String) {
        val animation = lottieAnimationsMap[animationName]
        if (animation === null) {
            if (animationTimerStarted) {
                return
            }
            animationTimer.scheduleAtFixedRate(
                timerTask {
                    animationTimerStarted = true
                    val lottieAnimationCur = lottieAnimationsMap[lottieAnimationsMap.keys.elementAt(defaultCurAnimation)]
                    runOnUiThread {
                        binding.lottieAnimation.setAnimation(lottieAnimationCur ?: R.raw.clear_day)
                        binding.lottieAnimation.playAnimation()
                        binding.lottieAnimation.repeatCount = LottieDrawable.INFINITE
                    }
                    defaultCurAnimation++

                    if (defaultCurAnimation > lottieAnimationsMap.keys.size - 1) {
                        defaultCurAnimation = 0
                    }
                },
                0,
                1000 * 3
            )
        } else {
            if (animationTimerStarted) {
                animationTimerStarted = false
                animationTimer.cancel()
            }
            runOnUiThread {
                binding.lottieAnimation.setAnimation(animation)
                binding.lottieAnimation.playAnimation()
                binding.lottieAnimation.repeatCount = LottieDrawable.INFINITE
            }
        }
    }

    private fun setUiColorScheme(background: Int, text: Int) {
        runOnUiThread {
            binding.background.setBackgroundResource(background)
            binding.temperature.setTextAppearance(text)
            binding.condition.setTextAppearance(text)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(applicationContext, background)
        }
    }
}
