package com.example.weatherapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.TransitionDrawable
import android.location.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentCurrentWeatherBinding
import com.example.weatherapp.model.adapters.DailyForecastAdapter
import com.example.weatherapp.model.adapters.HourlyForecastAdapter
import com.example.weatherapp.utils.*
import com.example.weatherapp.viewmodel.ForecastViewModel
import java.util.*


class CurrentWeather : Fragment() {

    private var _binding: FragmentCurrentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var forecastViewModel: ForecastViewModel

    private lateinit var adapterHourlyForecastList: HourlyForecastAdapter
    private lateinit var adapterDailyForecastList: DailyForecastAdapter

    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var hasAllPermissions = true
            for (permission in permissions) {
                if(!permission.value) {
                    hasAllPermissions = false
                }
            }

            if(hasAllPermissions) {
                if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    showAlert()
                } else {
                    getLocation()
                }
            }
        }

    override fun onStop() {
        super.onStop()
        locationManager.removeUpdates(locationListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentWeatherBinding.inflate(inflater, container, false)

        val preferencesManager = PreferencesManager.getInstance()

        val gradientDrawablesDayToNight = arrayOf(
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day),
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night),

        )

        val gradientDrawablesNightToDay = arrayOf(
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night),
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
        )

        forecastViewModel = ViewModelProvider(this).get(ForecastViewModel::class.java)

        forecastViewModel.getOneCallForecast(preferencesManager.lat!!, preferencesManager.lon!!)

        binding.mainLayout.apply {
            background = if (preferencesManager.useBackgroundDay) {
                ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
            } else {
                ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night)
            }

            visibility = View.VISIBLE
        }

        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            binding.progressBarLocation.visibility = View.GONE
            val lat = location.latitude
            val lon = location.longitude

            preferencesManager.lat = lat.toString()
            preferencesManager.lon = lon.toString()

            val gcd = Geocoder(requireContext(), Locale.ENGLISH)
            val addresses: List<Address>? = gcd.getFromLocation(lat, lon, 1)
            addresses?.let {
                if (addresses.isNotEmpty()) {
                    preferencesManager.city = addresses[0].locality
                    preferencesManager.countryCode = addresses[0].countryCode

                    binding.toolbarTitle.text = addresses[0].locality
                } else {
                    preferencesManager.city = convertCoordinates(lat, lon)
                    preferencesManager.countryCode = getString(R.string.unknown)
                }
            }

            forecastViewModel.getOneCallForecast(lat.toString(), lon.toString())
        }

        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigate(CurrentWeatherDirections.actionCurrentWeatherToPlacesList())
            }
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.use_location -> {
                        checkPermissions()
                        true
                    }

                    else -> false
                }
            }
        }

        binding.toolbarTitle.text = preferencesManager.city

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if(scrollY == 0){
                binding.currentWeatherCard.visibility = View.INVISIBLE
            } else {
                binding.currentWeatherCard.visibility = View.VISIBLE
            }
        })

        forecastViewModel.weatherData.observe(viewLifecycleOwner, { weather ->
            binding.weather = weather

            binding.scrollView.visibility = View.VISIBLE
            binding.scrollView.alpha = 0f
            binding.scrollView.animate().setDuration(600).alpha(1f)
        })

        forecastViewModel.responseData.observe(viewLifecycleOwner,  { response ->
            if(response != null) {
                binding.progressBarLocation.visibility = View.GONE
                locationManager.removeUpdates(locationListener)

                if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        activity?.window?.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        activity?.window?.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                    } else {
                        @Suppress("DEPRECATION")
                        activity?.window?.decorView?.systemUiVisibility = 0
                    }
                }

                val iconCode = response.current.weather[0].icon

                val oldBackground = preferencesManager.useBackgroundDay
                preferencesManager.useBackgroundDay = iconCode.last().toString() == "d"

                updateIcon(iconCode, binding.imageWeather)

                binding.mainLayout.apply {
                    if (preferencesManager.useBackgroundDay != oldBackground) {
                        if (preferencesManager.useBackgroundDay) {
                            val transitionDrawable = TransitionDrawable(gradientDrawablesNightToDay)
                            background = transitionDrawable
                            transitionDrawable.startTransition(1000)
                        } else {
                            val transitionDrawable = TransitionDrawable(gradientDrawablesDayToNight)
                            background = transitionDrawable
                            transitionDrawable.startTransition(1000)
                        }
                    } else {
                        if (preferencesManager.useBackgroundDay == oldBackground) {
                            background = if(preferencesManager.useBackgroundDay) {
                                ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
                            } else {
                                ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night)
                            }
                        }
                    }
                }

                response.hourly[0].temp = response.current.temp
                response.hourly[0].windSpeed = response.current.windSpeed
                response.hourly[0].weather[0].icon = response.current.weather[0].icon

                adapterHourlyForecastList = HourlyForecastAdapter(response.hourly.take(24))
                adapterDailyForecastList = DailyForecastAdapter(response.daily.take(5))

                binding.hourlyRecyclerView.apply {
                    adapter = adapterHourlyForecastList
                }

                binding.forecastRecyclerView.apply {
                    adapter = adapterDailyForecastList
                }

                forecastViewModel.responseData.postValue(null)
            }
        })

        return binding.root
    }

    private fun checkPermissions() {
        if(!permissionGranted(requireContext(), permissions)) {
            requestPermissions.launch(permissions)
        } else if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            showAlert()
        } else {
            getLocation()
        }
    }

    private fun permissionGranted(context: Context, permissions: Array<String>): Boolean {
        var hasAllPermissions = true
        for (permission in permissions) {
            val res = context.checkSelfPermission(permission)
            if (res != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false
            }
        }
        return hasAllPermissions
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        binding.progressBarLocation.visibility = View.VISIBLE
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
    }

    private fun showAlert() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Enable Location")
            .setMessage(
                """
            Your location setting is set to 'Off'.
            To continue, enable device location.
            """.trimIndent()
            )
            .setPositiveButton("Location Settings") { _, _ ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            .setNegativeButton("Cancel") { _, _ -> }
        dialog.show()
    }

}