package com.example.weatherapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.TransitionDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
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
import com.example.weatherapp.model.db.saved.SavedResponse
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.example.weatherapp.utils.*
import com.example.weatherapp.utils.Constants.Companion.LOCATION_SETTINGS_REQUEST
import com.example.weatherapp.viewmodel.CurrentWeatherViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import java.util.*


class CurrentWeather: Fragment() {

    private var _binding: FragmentCurrentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentWeatherViewModel: CurrentWeatherViewModel

    private val adapterHourlyForecastList: HourlyForecastAdapter by lazy {
        HourlyForecastAdapter()
    }

    private val adapterDailyForecastList: DailyForecastAdapter by lazy {
        DailyForecastAdapter()
    }

    private lateinit var networkConnection: NetworkConnectionListener

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var mRequest: LocationRequest
    private lateinit var mCallback: LocationCallback

    private var lastResponse: OneCallResponse? = null

    private var dataExpired = false

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
        if(lastResponse != null) {
            currentWeatherViewModel.upsertLastWeatherResponse(SavedResponse(
                city = preferencesManager.city!!,
                countryCode = preferencesManager.countryCode!!,
                oneCallResponse = lastResponse!!
            ))
        }
    }

    override fun onStart() {
        super.onStart()
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentWeatherBinding.inflate(inflater, container, false)

        currentWeatherViewModel = ViewModelProvider(this).get(CurrentWeatherViewModel::class.java)

        binding.mainLayout.background = if (preferencesManager.useBackgroundDay) {
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night)
        }

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

        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_currentWeather_to_locationsList)
            }
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.use_location -> {
                        it.isEnabled = false
                        checkPermissions()

                        view?.postDelayed({ it.isEnabled = true }, 500)
                        true
                    }

                    else -> false
                }
            }
        }

        binding.toolbarTitle.text = preferencesManager.city

        binding.hourlyRecyclerView.apply {
            adapter = adapterHourlyForecastList
        }

        binding.forecastRecyclerView.apply {
            adapter = adapterDailyForecastList
        }

        binding.refreshLayout.setOnRefreshListener {
            currentWeatherViewModel.getOneCallForecast(preferencesManager.lat, preferencesManager.lon)
        }

        binding.refreshButton.setOnClickListener {
            currentWeatherViewModel.getOneCallForecast(preferencesManager.lat, preferencesManager.lon)
        }

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if(scrollY == 0){
                binding.refreshLayout.isEnabled = true
                binding.currentWeatherCard.visibility = View.INVISIBLE
            } else {
                binding.refreshLayout.isEnabled = false
                binding.currentWeatherCard.visibility = View.VISIBLE
            }
        })

        currentWeatherViewModel.savedWeatherData.observe(viewLifecycleOwner, { savedForecast ->
            if(savedForecast != null) {
                handleResponse(savedForecast)

                if(timestampOlderThanTenMin(savedForecast.current.dt)) {
                    dataExpired = true
                    currentWeatherViewModel.getOneCallForecast(preferencesManager.lat, preferencesManager.lon)
                } else {
                    dataExpired = false
                }
            } else {
                currentWeatherViewModel.getOneCallForecast(preferencesManager.lat, preferencesManager.lon)
            }
        })

        currentWeatherViewModel.responseData.observe(viewLifecycleOwner,  { response ->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()

                    response.data?.let { responseBody ->
                        lastResponse = responseBody
                        handleResponse(responseBody)
                    }
                }

                is Resource.Error -> {
                    response.message?.let { event ->
                        event.getContentIfNotHandledOrReturnNull()?.let {
                            hideProgressBar()
                            if(dataExpired) {
                                Toast.makeText(requireContext(), requireContext().resources.getString(R.string.outdated_data), Toast.LENGTH_SHORT).show()
                            }
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
            if(!preferencesManager.askedLocationPermission) {
                requestPermissions.launch(permissions)
                preferencesManager.askedLocationPermission = true
            } else {
                if(shouldShowRequestPermissionRationale(permissions[0]) || shouldShowRequestPermissionRationale(permissions[1])) {
                    requestPermissions.launch(permissions)
                } else {
                    val snackbar = Snackbar.make(binding.mainLayout, "App permission not granted", Snackbar.LENGTH_LONG)
                        .setAction(this.resources.getString(R.string.settings)) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", activity?.packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }

                    snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
                    snackbar.show()
                }
            }
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
                        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                    }

                    val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(mRequest)

                    val client = LocationServices.getSettingsClient(activity as MainActivity)
                    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

                    task.addOnFailureListener { e ->
                        if (e is ResolvableApiException) {
                            try {
                                e.startResolutionForResult(activity as MainActivity, LOCATION_SETTINGS_REQUEST)
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

        val gcd = Geocoder(requireContext(), Locale.ENGLISH)
        val addresses: List<Address>? = gcd.getFromLocation(lat, lon, 1)
        addresses?.let {
            if (addresses.isNotEmpty()) {
                currentWeatherViewModel.getCityDataToAddByCoord(lat, lon, addresses[0].locality, addresses[0].countryCode)
                binding.toolbarTitle.text = addresses[0].locality
            } else {
                Toast.makeText(requireContext(), requireContext().getText(R.string.unknown_location), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleResponse(response: OneCallResponse) {
        preferencesManager.timeZone = response.timezone

        val currentData = response.current
        val iconCode = currentData.weather[0].icon

        val oldBackground = preferencesManager.useBackgroundDay
        preferencesManager.useBackgroundDay = iconCode.last().toString() == "d"

        val gradientDrawablesDayToNight = arrayOf(
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day),
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night),
        )

        val gradientDrawablesNightToDay = arrayOf(
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_night),
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_background_day)
        )

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
            }
        }

        adapterHourlyForecastList.setData(response.hourly.take(24))
        adapterDailyForecastList.setData(response.daily.take(5))
        binding.current = currentData

        binding.sunProgressBar.setPercent(getSunProgress(currentData.dt, currentData.sunrise, currentData.sunset))

        binding.scrollView.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().setDuration(600).alpha(1f)
        }
    }

}