package com.example.weatherapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager private constructor(context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    var city: String?
        get() = sharedPrefs.getString(KEY_CITY, "Warsaw")

        set(value) {
            sharedPrefs.edit {
                putString(KEY_CITY, value)
                apply()
            }
        }

    var countryCode: String?
        get() = sharedPrefs.getString(KEY_COUNTRY_CODE, "PL")

        set(value) {
            sharedPrefs.edit {
                putString(KEY_COUNTRY_CODE, value)
                apply()
            }
        }

    var lat: String?
        get() = sharedPrefs.getString(KEY_LAT, defaultLat)

        set(value) {
            sharedPrefs.edit {
                putString(KEY_LAT, value)
                apply()
            }
        }

    var lon: String?
        get() = sharedPrefs.getString(KEY_LON, defaultLon)

        set(value) {
            sharedPrefs.edit {
                putString(KEY_LON, value)
                apply()
            }
        }

    var timeZone: String?
        get() = sharedPrefs.getString(KEY_TIMEZONE, "")

        set(value) {
            sharedPrefs.edit {
                putString(KEY_TIMEZONE, value)
                apply()
            }
        }

    var useBackgroundDay: Boolean
        get() = sharedPrefs.getBoolean(KEY_USE_BACKGROUND_DAY, true)

        set(value) {
            sharedPrefs.edit {
                putBoolean(KEY_USE_BACKGROUND_DAY, value)
                apply()
            }
        }

    var isSwipeEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_SWIPE_ENABLED, true)

        set(value) {
            sharedPrefs.edit {
                putBoolean(KEY_SWIPE_ENABLED, value)
                apply()
            }
        }

    companion object {
        const val KEY_CITY = "KEY_CITY"
        const val KEY_COUNTRY_CODE = "KEY_COUNTRY_CODE"
        const val KEY_LAT = "KEY_LAT"
        const val KEY_LON = "KEY_LON"
        const val KEY_TIMEZONE = "KEY_TIMEZONE"
        const val KEY_USE_BACKGROUND_DAY = "KEY_USE_BACKGROUND_DAY"
        const val KEY_SWIPE_ENABLED = "KEY_SWIPE_ENABLED"

        const val defaultLat = "52.2298"
        const val defaultLon = "21.0118"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun init(context: Context): PreferencesManager {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = PreferencesManager(context)
                }
            }

            return getInstance()
        }

        fun getInstance(): PreferencesManager {
            val instance = INSTANCE

            if (instance != null) {
                return instance
            }

            error("PreferencesManager must be initialized first.")
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}
}