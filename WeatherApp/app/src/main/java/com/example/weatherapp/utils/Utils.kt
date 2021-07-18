package com.example.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*
import android.widget.ImageView
import com.example.weatherapp.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


val preferencesManager = PreferencesManager.getInstance()

fun timeFormat(value: Int, givenTimeZone: String): String {
    val zoneId: ZoneId = ZoneId.of(givenTimeZone)
    val time: LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(value.toLong() * 1000),
        zoneId
    )
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    return time.format(formatter)
}

@SuppressLint("SimpleDateFormat")
fun getDayOfWeek(value: Int): String {
    val sdf = java.text.SimpleDateFormat("E", Locale.ENGLISH)
    val dayOfWeek =  Date(value.toLong().times(1000))

    return sdf.format(dayOfWeek).capitalizeFirst
}

fun convertWindUnit(value: Double): String {
    val newValue = BigDecimal(value * 3.6).setScale(1, RoundingMode.HALF_EVEN)
    val df = DecimalFormat("#0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
    df.isDecimalSeparatorAlwaysShown = false

    return df.format(newValue)
}

val String.capitalizeFirst: String get() {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}

fun round(value: Double): String {
    val df = DecimalFormat("#0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
    df.isDecimalSeparatorAlwaysShown = false

    return df.format(value)
}

fun convertCoordinates(latitude: Double, longitude: Double) : String {
    var latSeconds = (latitude * 3600).roundToInt()
    val latDegrees = latSeconds / 3600
    latSeconds = abs(latSeconds % 3600)
    val latMinutes = latSeconds / 60
    latSeconds %= 60

    var longSeconds = (longitude * 3600).roundToInt()
    val longDegrees = longSeconds / 3600
    longSeconds = abs(longSeconds % 3600)
    val longMinutes = longSeconds / 60
    longSeconds %= 60

    val latDirection = if (latDegrees >= 0) "N" else "S"
    val lonDirection = if (longDegrees >= 0) "E" else "W"

    return "${abs(latDegrees)}°$latMinutes'$latSeconds\" $latDirection, ${abs(longDegrees)}°$longMinutes'$longSeconds\" $lonDirection"
}

fun getSunProgress(currentTime: Int, sunrise: Int, sunset: Int) : Int {
    val sunriseNowDiff = ((currentTime - sunrise) / 60).toDouble()
    val sunsetSunriseDiff = ((sunset - sunrise) / 60).toDouble()

    return if(currentTime in (sunrise + 1) until sunset) {
        ((sunriseNowDiff / sunsetSunriseDiff) * 100).toInt()
    } else {
        0
    }
}

fun updateIcon(iconCode: String?, imageView: ImageView){
    val newIconCode = iconCode
        ?.replace("n","")
        ?.replace("d","")

    if(newIconCode == "01" || newIconCode == "02"){
        when(iconCode) {
            "01d" -> imageView.setImageResource(R.drawable.ic_clear)
            "01n" -> imageView.setImageResource(R.drawable.ic_moon)
            "02d" -> imageView.setImageResource(R.drawable.ic_few_clouds_day)
            "02n" -> imageView.setImageResource(R.drawable.ic_few_clouds_night)
        }
    } else {
        when(newIconCode) {
            "03" -> imageView.setImageResource(R.drawable.ic_scattered_clouds)
            "04" -> {
                if(preferencesManager.useBackgroundDay) {
                    imageView.setImageResource(R.drawable.ic_broken_clouds_day)
                } else {
                    imageView.setImageResource(R.drawable.ic_broken_clouds_night)
                }
            }
            "09" -> {
                if(preferencesManager.useBackgroundDay) {
                    imageView.setImageResource(R.drawable.ic_shower_rain_day)
                } else {
                    imageView.setImageResource(R.drawable.ic_shower_rain_night)
                }
            }
            "10" -> {
                if(preferencesManager.useBackgroundDay) {
                    imageView.setImageResource(R.drawable.ic_rain_day)
                } else {
                    imageView.setImageResource(R.drawable.ic_rain_night)
                }
            }
            "11" -> imageView.setImageResource(R.drawable.ic_storm)
            "13" -> imageView.setImageResource(R.drawable.ic_snow)
            "50" -> imageView.setImageResource(R.drawable.ic_mist)
        }

    }
}

fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return when {
        capabilities.hasTransport(TRANSPORT_WIFI) -> true
        capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
        capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

sealed class Resource<T>(
    val data: T? = null,
    val message: Event<String>? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: Event<String>, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}

open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    fun getContentIfNotHandledOrReturnNull(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
