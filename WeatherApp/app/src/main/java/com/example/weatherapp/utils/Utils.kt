package com.example.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import com.example.weatherapp.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*


val preferencesManager = PreferencesManager.getInstance()

@BindingAdapter("weatherIcon")
fun ImageView.bindIcon(iconCode: String?) {
    iconCode?.let {
        updateIcon(iconCode, this)
    }
}

@BindingAdapter("textHour")
fun TextView.textHour(dt: Int?) {
    dt?.let {
        this.text = timeFormat(dt, preferencesManager.timeZone!!)
    }
}

@BindingAdapter("textTemp")
fun TextView.textTemp(value: Double?) {
    value?.let {
        this.text = String.format(context.resources.getString(R.string.val_feels_like_degrees_celsius, round(value)))
    }
}

@BindingAdapter("textWindSpeed")
fun TextView.textWindSpeed(windSpeed: Double?) {
    windSpeed?.let {
        this.text = String.format(context.resources.getString(R.string.val_kmh, convertWindUnit(windSpeed)))
    }
}

@BindingAdapter("textWeekDay")
fun TextView.textWeekDay(dt: Int?) {
    dt?.let {
        this.text = getDayOfWeek(dt)
    }
}

fun getStatusBarHeight(context: Context): Int {
    val statusBarHeight = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (statusBarHeight > 0) {
        context.resources.getDimensionPixelSize(statusBarHeight)
    } else {
        0
    }
}

fun timestampOlderThanTenMin(dt: Int): Boolean{
    val tenMin = 10 * 60 * 1000

    val tenMinAgo: Long = (System.currentTimeMillis() - tenMin) / 1000L
    return dt < tenMinAgo
}

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

    return round(newValue.toDouble())
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

fun getSunProgress(currentTime: Int, sunrise: Int, sunset: Int) : Int {
    val sunriseNowDiff = ((currentTime - sunrise) / 60).toDouble()
    val sunsetSunriseDiff = ((sunset - sunrise) / 60).toDouble()

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val day =  Date(currentTime.toLong().times(1000))

    val localDate = LocalDate.parse(sdf.format(day))
    val dayEnd = LocalDateTime.of(localDate, LocalTime.MAX)
    val dayEndUnix = dayEnd.atZone(ZoneId.systemDefault()).toEpochSecond()

    return when(currentTime) {
        in (sunrise + 1) until sunset -> {
            ((sunriseNowDiff / sunsetSunriseDiff) * 100).toInt()
        }

        in (sunset + 1) until dayEndUnix -> {
            100
        }

        else -> 0
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

fun adjustTheme(context: Context, activity: FragmentActivity) {
    if(context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window?.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        } else {
            @Suppress("DEPRECATION")
            activity.window?.decorView?.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}

fun enableSearchView(view: View, enabled: Boolean) {
    view.isEnabled = enabled
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            enableSearchView(child, enabled)
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
