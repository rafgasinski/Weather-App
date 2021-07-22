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

    var locationId: Int
        get() = sharedPrefs.getInt(KEY_LOCATION_ID, 0)

        set(value) {
            sharedPrefs.edit {
                putInt(KEY_LOCATION_ID, value)
                apply()
            }
        }

    var city: String?
        get() = sharedPrefs.getString(KEY_CITY, "")

        set(value) {
            sharedPrefs.edit {
                putString(KEY_CITY, value)
                apply()
            }
        }

    var countryCode: String?
        get() = sharedPrefs.getString(KEY_COUNTRY_CODE, "")

        set(value) {
            sharedPrefs.edit {
                putString(KEY_COUNTRY_CODE, value)
                apply()
            }
        }

    var lat: Double
        get() = java.lang.Double.longBitsToDouble(sharedPrefs.getLong(KEY_LAT, 0))

        set(value) {
            sharedPrefs.edit {
                putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(value))
                apply()
            }
        }

    var lon: Double
        get() = java.lang.Double.longBitsToDouble(sharedPrefs.getLong(KEY_LON, 0))

        set(value) {
            sharedPrefs.edit {
                putLong(KEY_LON, java.lang.Double.doubleToRawLongBits(value))
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

    var askedLocationPermission: Boolean
        get() = sharedPrefs.getBoolean(KEY_ASKED_LOCATION_PERMISSION, false)

        set(value) {
            sharedPrefs.edit {
                putBoolean(KEY_ASKED_LOCATION_PERMISSION, value)
                apply()
            }
        }

    companion object {
        const val KEY_LOCATION_ID = "KEY_LOCATION_ID"
        const val KEY_CITY = "KEY_CITY"
        const val KEY_COUNTRY_CODE = "KEY_COUNTRY_CODE"
        const val KEY_LAT = "KEY_LAT"
        const val KEY_LON = "KEY_LON"
        const val KEY_TIMEZONE = "KEY_TIMEZONE"
        const val KEY_USE_BACKGROUND_DAY = "KEY_USE_BACKGROUND_DAY"
        const val KEY_ASKED_LOCATION_PERMISSION = "KEY_ASKED_LOCATION_PERMISSION"

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