package com.example.weatherapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.TransitionDrawable
import android.location.*
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.weatherapp.MainActivity
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentCurrentWeatherBinding
import com.example.weatherapp.model.adapters.DailyForecastAdapter
import com.example.weatherapp.model.adapters.HourlyForecastAdapter
import com.example.weatherapp.model.data.WeatherData
import com.example.weatherapp.utils.*
import com.example.weatherapp.viewmodel.ForecastViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.util.*
import kotlin.math.roundToInt


class CurrentWeather : Fragment() {

    private var _binding: FragmentCurrentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var forecastViewModel: ForecastViewModel

    private lateinit var adapterHourlyForecastList: HourlyForecastAdapter
    private lateinit var adapterDailyForecastList: DailyForecastAdapter

    private lateinit var networkConnection: NetworkConnectionListener

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var mRequest: LocationRequest
    private lateinit var mCallback: LocationCallback

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
                getLocation()
            }
        }

    override fun onStop() {
        super.onStop()
        fusedClient.removeLocationUpdates(mCallback)
    }

    override fun onStart() {
        super.onStart()
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
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

        binding.mainLayout.background = if (preferencesManager.useBackgroundDay) {
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night)
        }

        forecastViewModel.getOneCallForecast(preferencesManager.lat!!, preferencesManager.lon!!)

        networkConnection = NetworkConnectionListener(requireContext())
        networkConnection.observe(viewLifecycleOwner, { connected ->
            if(!connected) {
                hideProgressBar()
            }
        })

        if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                activity?.window?.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            } else {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = 0
            }
        }

        mCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    manageLocationData(location)
                }
                fusedClient.removeLocationUpdates(mCallback)
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if(locationAvailability.isLocationAvailable) {
                    showProgressBar()
                }
            }
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

        binding.refreshButton.setOnClickListener {
            forecastViewModel.getOneCallForecast(preferencesManager.lat!!, preferencesManager.lon!!)
        }

        binding.toolbarTitle.text = preferencesManager.city

        binding.refreshLayout.setOnRefreshListener {
            forecastViewModel.getOneCallForecast(preferencesManager.lat!!, preferencesManager.lon!!)
        }

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if(scrollY == 0){
                binding.currentWeatherCard.visibility = View.INVISIBLE
            } else {
                binding.currentWeatherCard.visibility = View.VISIBLE
            }
        })

        forecastViewModel.responseData.observe(viewLifecycleOwner,  { response ->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()

                    response.data?.let { responseBody ->
                        preferencesManager.timeZone = responseBody.timezone

                        val currentData = responseBody.current

                        val iconCode = currentData.weather[0].icon

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

                        responseBody.hourly[0].temp = currentData.temp
                        responseBody.hourly[0].windSpeed = currentData.windSpeed
                        responseBody.hourly[0].weather[0].icon = currentData.weather[0].icon

                        adapterHourlyForecastList = HourlyForecastAdapter(responseBody.hourly.take(24))
                        adapterDailyForecastList = DailyForecastAdapter(responseBody.daily.take(5))

                        binding.hourlyRecyclerView.apply {
                            adapter = adapterHourlyForecastList
                        }

                        binding.forecastRecyclerView.apply {
                            adapter = adapterDailyForecastList
                        }

                        forecastViewModel.responseData.postValue(null)

                        binding.weather = WeatherData(
                            currentData.temp.roundToInt().toString(),
                            currentData.weather[0].description.capitalizeFirst,
                            timeFormat(currentData.sunrise, responseBody.timezone),
                            timeFormat(currentData.sunset, responseBody.timezone),
                            round(currentData.feelsLike),
                            currentData.clouds.roundToInt().toString(),
                            convertWindUnit(currentData.windSpeed),
                            currentData.humidity.toString(),
                            currentData.pressure.toString(),
                            currentData.uvi.roundToInt().toString(),
                            getSunProgress(currentData.dt, currentData.sunrise, currentData.sunset)
                        )

                        binding.scrollView.visibility = View.VISIBLE
                        binding.scrollView.alpha = 0f
                        binding.scrollView.animate().setDuration(600).alpha(1f)

                    }
                }

                is Resource.Error -> {
                    response.message?.let { event ->
                        event.getContentIfNotHandledOrReturnNull()?.let {
                            hideProgressBar()
                            showErrorLayout(it)
                        }
                    }
                }

                is Resource.Loading -> {
                    hideErrorLayout()
                    if(!binding.refreshLayout.isRefreshing) {
                        showProgressBar()
                    }
                }
            }
        })

        return binding.root
    }

    private fun showErrorLayout(message: String) {
        if(binding.scrollView.visibility == View.VISIBLE){
            binding.error.visibility = View.GONE
            binding.refreshButton.visibility = View.GONE

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } else {
            binding.error.alpha = 0f
            binding.refreshButton.alpha = 0f

            binding.error.animate().setDuration(600).alpha(1f).withStartAction {
                binding.error.text = message
                binding.error.visibility = View.VISIBLE
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.animate().setDuration(600).alpha(1f)
            }
        }
    }

    private fun hideErrorLayout() {
        binding.error.visibility = View.GONE
        binding.refreshButton.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBarLocation.visibility = View.VISIBLE
        binding.refreshLayout.isEnabled = false
        binding.refreshLayout.isRefreshing = false
    }

    private fun hideProgressBar() {
        binding.progressBarLocation.visibility = View.GONE
        binding.refreshLayout.isEnabled = true
        binding.refreshLayout.isRefreshing = false
    }

    private fun checkPermissions() {
        if(!permissionGranted(requireContext(), permissions)) {
            requestPermissions.launch(permissions)
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
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (isOnline(requireContext())) {
                if (location != null) {
                    manageLocationData(location)
                } else {
                    mRequest = LocationRequest.create().apply {
                        interval = 10000
                        fastestInterval = 5000
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    }

                    val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(mRequest)

                    val client = LocationServices.getSettingsClient(activity as MainActivity)
                    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

                    task.addOnFailureListener { e ->
                        if (e is ResolvableApiException) {
                            try {
                                e.startResolutionForResult(activity as MainActivity, 500)
                            } catch (sendEx: SendIntentException) {}
                        }
                    }

                    fusedClient.requestLocationUpdates(mRequest, mCallback, Looper.getMainLooper())
                }
            } else {
                showErrorLayout(requireContext().resources.getString(R.string.no_network_connection))
            }
        }
    }

    private fun manageLocationData(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        preferencesManager.lat = lat.toString()
        preferencesManager.lon = lon.toString()

        val gcd = Geocoder(requireContext(), Locale.ENGLISH)
        val addresses: List<Address>? = gcd.getFromLocation(lat, lon, 1)
        addresses?.let {
            if (addresses.isNotEmpty()) {
                binding.toolbarTitle.text = addresses[0].locality
                preferencesManager.city = addresses[0].locality
                preferencesManager.countryCode = addresses[0].countryCode
            } else {
                preferencesManager.city = convertCoordinates(lat, lon)
                preferencesManager.countryCode = getString(R.string.unknown)
            }
        }

        forecastViewModel.getOneCallForecast(lat.toString(), lon.toString())
    }

}