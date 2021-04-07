package com.example.weatherapp.view

import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Constants
import com.example.weatherapp.R
import com.example.weatherapp.model.adapters.DailyForecastAdapter
import com.example.weatherapp.model.adapters.HourlyForecastAdapter
import com.example.weatherapp.model.response.OneCallResponse
import com.example.weatherapp.viewmodel.ForecastViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.android.synthetic.main.fragment_accessibility_current_weather.*
import kotlinx.android.synthetic.main.fragment_current_weather.clouds_value
import kotlinx.android.synthetic.main.fragment_current_weather.feelsLike_value
import kotlinx.android.synthetic.main.fragment_current_weather.forecast_recycler_view
import kotlinx.android.synthetic.main.fragment_current_weather.hourly_recycler_view
import kotlinx.android.synthetic.main.fragment_current_weather.humidity_value
import kotlinx.android.synthetic.main.fragment_current_weather.imageTemp
import kotlinx.android.synthetic.main.fragment_current_weather.pressure_value
import kotlinx.android.synthetic.main.fragment_current_weather.sunrise_value
import kotlinx.android.synthetic.main.fragment_current_weather.sunset_value
import kotlinx.android.synthetic.main.fragment_current_weather.textCity
import kotlinx.android.synthetic.main.fragment_current_weather.textDate
import kotlinx.android.synthetic.main.fragment_current_weather.textDescription
import kotlinx.android.synthetic.main.fragment_current_weather.textTemp
import kotlinx.android.synthetic.main.fragment_current_weather.uvIndex_value
import kotlinx.android.synthetic.main.fragment_current_weather.windSpeed_value
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class CurrentWeatherAccessibility : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // view model
    private lateinit var forecastViewModel: ForecastViewModel

    // view managers, adapters
    lateinit var viewManagerHourly: RecyclerView.LayoutManager
    lateinit var adapterHourlyForecastList: HourlyForecastAdapter

    lateinit var viewManagerDaily: RecyclerView.LayoutManager
    lateinit var adapterDailyForecastList: DailyForecastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        forecastViewModel = ViewModelProvider(this).get(ForecastViewModel::class.java)

        return inflater.inflate(R.layout.fragment_accessibility_current_weather, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Navigation setup, changing main view in SharedPreferences
         * */

        switchViewBack.setOnClickListener{
            val sharedPref: SharedPreferences? = activity?.getSharedPreferences(Constants.SH_ACCESSIBILITY_KEY, Constants.PRIVATE_MODE)

            val editor = sharedPref?.edit()
            editor?.putBoolean(Constants.SH_ACCESSIBILITY_KEY, false)
            editor?.commit()

            it.findNavController().navigate(R.id.action_currentWeatherAccessibility_to_currentWeather)
        }

        viewManagerHourly = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        viewManagerDaily = LinearLayoutManager(requireContext())

        forecast_recycler_view.setHasFixedSize(true)
        hourly_recycler_view.setHasFixedSize(true)

        getForecastAndUpdateView()

        /**
         * Setup autocomplete fragment, Places API
         * */
        val apiKey = Constants.PLACES_API_KEY

        if (!Places.isInitialized()) {
            activity?.let { Places.initialize(it, apiKey) }
        }

        val placesClient = activity?.let { Places.createClient(it) }

        val autocompleteFragment =
                childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                        as AutocompleteSupportFragment

        autocompleteFragment.view?.setBackgroundColor(4292335575.toInt())

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.ID, Place.Field.NAME))

        autocompleteFragment.setTypeFilter(TypeFilter.CITIES)

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val placeCoords = place.latLng

                val sharedPref: SharedPreferences? = activity?.getSharedPreferences(Constants.SH_LAST_LAT_LON_KEY, Constants.PRIVATE_MODE)
                val editor = sharedPref?.edit()
                editor?.putString(Constants.SH_LAST_LAT_LON_KEY, "${placeCoords?.latitude},${placeCoords?.longitude}")
                editor?.putString(Constants.SH_LAST_CITY_KEY, "${place.name}")
                editor?.putString(Constants.SH_LAST_PLACE_ID_KEY, "${place.id}")
                editor?.commit()

                getForecastAndUpdateView()
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(ContentValues.TAG, "An error occurred: $status")
            }

        })
    }

    /**
     * Getting response from OpenWeather API based on SharedPrefences key
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getForecastAndUpdateView(){
        try{

            var sharedPref: SharedPreferences? = activity?.getSharedPreferences(Constants.SH_LAST_LAT_LON_KEY, Constants.PRIVATE_MODE)

            val latLon = sharedPref?.getString(Constants.SH_LAST_LAT_LON_KEY, "${ForecastViewModel.defaultLat},${ForecastViewModel.defaultLon}")
            Log.d("latLon", latLon.toString())
            val arrayData = latLon!!.split(",").toTypedArray()

            forecastViewModel.getOneCallForecast(arrayData[0], arrayData[1])

            textCity.text = sharedPref?.getString(Constants.SH_LAST_CITY_KEY, "Warsaw")

        } catch(ex: Exception){
            Toast.makeText(requireContext(), ex.message, Toast.LENGTH_SHORT).show()
        }
        forecastViewModel.responseBody.observe(viewLifecycleOwner, Observer { response ->
            updateCurrentDayData(response)

            response.hourly[0].temp = response.current.temp
            response.hourly[0].windSpeed = response.current.windSpeed
            response.hourly[0].weather[0].icon = response.current.weather[0].icon

            adapterHourlyForecastList = HourlyForecastAdapter(response.hourly.take(24), forecastViewModel)
            adapterDailyForecastList = DailyForecastAdapter(response.daily.take(5), forecastViewModel)

            hourly_recycler_view.apply {
                adapter = adapterHourlyForecastList
                layoutManager = viewManagerHourly
            }

            forecast_recycler_view.apply {
                adapter = adapterDailyForecastList
                layoutManager = viewManagerDaily
            }
        })
    }

    /**
     * Update view data with current weather for selected day
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateCurrentDayData(oneCallResponse: OneCallResponse){

        val current = oneCallResponse.current

        updateIcon(current.weather[0].icon)

        val sdf = java.text.SimpleDateFormat("HH:mm")
        val sunrise = java.util.Date(current.sunrise.toLong() * 1000)
        val sunset = java.util.Date(current.sunset.toLong() * 1000)

        sunrise_value.text = sdf.format(sunrise)
        sunset_value.text = sdf.format(sunset)

        val sdfWholeDate = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
        val date = java.util.Date(current.dt.toLong() * 1000)
        textDate.text = sdfWholeDate.format(date)

        val description = current.weather[0].description
        textDescription.text = description[0].toUpperCase()+description.substring(1)

        textTemp.text = "${current.temp.roundToInt()}°C"

        feelsLike_value.text = "${current.feelsLike.roundToInt()}°C"

        humidity_value.text = "${current.humidity}%"

        clouds_value.text = "${current.clouds.roundToInt()}%"

        val decimalWindSpeed = BigDecimal(current.windSpeed * 3.6).setScale(1, RoundingMode.HALF_EVEN)
        val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
        df.isDecimalSeparatorAlwaysShown = false
        windSpeed_value.text = "${df.format(decimalWindSpeed)} km/h"

        pressure_value.text = "${current.pressure}hPa"

        uvIndex_value.text = current.uvi.roundToInt().toString()
    }

    fun Long.getDateTimeFormatFromSec(format: String = Constants.DATE_TIME_FORMAT): String
    {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            Instant.ofEpochMilli(this * 1000)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime()
                    .format(
                            DateTimeFormatter.ofPattern(
                                    format,
                                    Locale.getDefault()
                            )
                    )
        }
        else
        {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            val calendar = Calendar.getInstance().apply { timeInMillis = this@getDateTimeFormatFromSec }
            return formatter.format(calendar.time)
        }
    }

    /**
     * Updates the weather icon with respect to the icon code
     * @param iconCode icon code that goes with API data
     * */
    private fun updateIcon(iconCode: String){
        // removes "n" and "d" from code because icons are based only on daytime
        val newIconCode = iconCode
                .replace("n","")
                .replace("d","")

        when(newIconCode){
            "01" -> imageTemp.setImageResource(R.drawable.icon_clear_sky)
            "02" -> imageTemp.setImageResource(R.drawable.icon_few_clouds)
            "03" -> imageTemp.setImageResource(R.drawable.icon_scattered_clouds)
            "04" -> imageTemp.setImageResource(R.drawable.icon_broken_clouds)
            "09" -> imageTemp.setImageResource(R.drawable.icon_shower_rain)
            "10" -> imageTemp.setImageResource(R.drawable.icon_rain)
            "11" -> imageTemp.setImageResource(R.drawable.icon_thunderstorm)
            "13" -> imageTemp.setImageResource(R.drawable.icon_snow)
            "50" -> imageTemp.setImageResource(R.drawable.icon_mist)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Settings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                CurrentWeather().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}