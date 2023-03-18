package com.sarthakpranesh.simplyweather

import android.app.Activity
import android.location.*
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.airbnb.lottie.LottieDrawable
import com.sarthakpranesh.simplyweather.databinding.ActivityMainBinding
import com.sarthakpranesh.simplyweather.location.Location
import com.sarthakpranesh.simplyweather.location.LocationPermission
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

val defaultWeatherDetails = WeatherDetails("Loading", "", "", "", true)

class MainActivity : Activity() {
    // defaults
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

    // class variables
    private lateinit var locationPermission: LocationPermission
    private lateinit var location: Location

    private var defaultCurAnimation = 0
    private lateinit var weather: WeatherDetails
    private var animationTimer: Timer = Timer()
    private var animationTimerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        weather = defaultWeatherDetails

        locationPermission = LocationPermission(this)
        location = Location(applicationContext)

        setUiColorScheme(R.color.rain_bg, R.color.rain_text)
        binding.condition.text = getString(R.string.lookingForCloudsAround)
        binding.temperature.text = getString(R.string.loading)
        setLottieAnimation("random")
    }

    override fun onResume() {
        super.onResume()

        if (!locationPermission.isLocationPermissionGranted()) {
            locationPermission.requestLocationPermission()
            binding.condition.text = "Please provide location permission to use the app!"
            binding.temperature.text = "No Permission"
            setLottieAnimation("random")
            return
        } else {
            location.subscribeToUserLocation()
        }

        if (weather.loading) {
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            binding.condition.text = getString(R.string.lookingForCloudsAround)
            binding.temperature.text = getString(R.string.loading)
            setLottieAnimation("random")
        }

        CoroutineScope(Dispatchers.IO).launch {
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    // fetch and update weather details in data class
                    val wasSuccess = getWeatherDetails()
                    Log.i("Weather", weather.toString())
                    // update ui
                    if (wasSuccess) {
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.temperature.text = weather.temperature
                            binding.condition.text = "${weather.condition} in ${weather.city}"
                            UiLogic.understandAndSetLottie(
                                weather,
                                ::setUiColorScheme,
                                ::setLottieAnimation
                            )
                        }
                    }
                }
            }, 0, 1000 * 60 * 1)
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
    private fun getWeatherDetails(): Boolean {
        val clint = OkHttpClient()
        try {
            val location = location.getUserLocation()

            // enforce location retrieval
            if (location == null) {
                Log.i("Weather", "Failed")
                allErrorHandler(message = "Unable to retrieve device location! Please provide location access.")
                return false
            }

            // enforce location address retrieval
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
            } else {
                allErrorHandler()
                return true
            }
            response.close()
            return true
        } catch (e: IOException) {
            // do something on exception
            Log.i("Error", e.message.toString())
            return false
        }
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
