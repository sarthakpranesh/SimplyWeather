package com.sarthakpranesh.simplyweather

import java.util.*

object UiLogic {
    fun understandAndSetLottie(
        weather: WeatherDetails,
        setUiColorScheme: (background: Int, text: Int) -> Unit,
        setLottieAnimation: (t: String) -> Unit
    ) {
        // check for storm or thunderstorm
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("thunderstorm", "storm"))) {
            setLottieAnimation("storm")
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // check for thunder
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("thunder"))) {
            setLottieAnimation("thunder")
            setUiColorScheme(R.color.rain_bg, R.color.rain_text)
            return
        }

        // check for windy
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("windy"))) {
            setLottieAnimation("windy")
            setUiColorScheme(R.color.windy_bg, R.color.windy_text)
            return
        }

        // check for mist
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("mist", "haze", "smoke"))) {
            setLottieAnimation("mist")
            setUiColorScheme(R.color.cloudy_bg, R.color.cloudy_text)
            return
        }

        // check for clear
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("clear", "sunny"))) {
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
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("cloudy", "cloud", "overcast"))) {
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
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("rainy", "rain"))) {
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
        if (doesAnyKeyExistInWeatherCondition(weather, arrayOf("snow"))) {
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

    private fun doesAnyKeyExistInWeatherCondition(weather: WeatherDetails, keys: Array<String>): Boolean {
        keys.forEach {
            if (weather.condition.lowercase().contains(it, true)) {
                return true
            }
        }
        return false
    }
}