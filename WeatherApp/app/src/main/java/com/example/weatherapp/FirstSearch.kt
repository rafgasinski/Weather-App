package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.databinding.ActivityFirstSearchBinding
import com.example.weatherapp.utils.*
import com.example.weatherapp.utils.Constants.Companion.LOCATION_SETTINGS_REQUEST
import com.example.weatherapp.viewmodel.FirstSearchViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.util.*


class FirstSearch: AppCompatActivity() {

    private lateinit var binding: ActivityFirstSearchBinding
    private lateinit var firstSearchViewModel: FirstSearchViewModel

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var mRequest: LocationRequest
    private lateinit var mCallback: LocationCallback

    private lateinit var networkConnection: NetworkConnectionListener

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

    override fun onStart() {
        super.onStart()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFirstSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firstSearchViewModel = ViewModelProvider(this).get(FirstSearchViewModel::class.java)

        adjustTheme(this, this)

        networkConnection = NetworkConnectionListener(this)
        networkConnection.observe(this, { connected ->
            if(!connected) {
                hideProgressBar()
            }
        })

        binding.mainLayout.apply {
            alpha = 0f
            animate().setDuration(600).alpha(1f)
        }

        binding.topLayout.setPadding(0, getStatusBarHeight(this), 0, 0)

        binding.searchView.apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(textInput: String?): Boolean {
                        textInput?.let {
                            firstSearchViewModel.getCityDataToAddByName(textInput)
                            showOverlay()
                        }
                        return false
                    }

                    override fun onQueryTextChange(textInput: String?): Boolean {
                        return false
                    }
                }
            )

            isIconified = false
        }

        KeyboardVisibilityEvent.setEventListener(this, this) { isOpen ->
            if(isOpen) {
                binding.hint.startAnimation(AnimationUtils.loadAnimation(this, R.anim.move_up))
            } else {
                binding.hint.startAnimation(AnimationUtils.loadAnimation(this, R.anim.move_down))
            }
        }

        binding.location.setOnClickListener {
            checkPermissions()
        }

        firstSearchViewModel.shouldNavigate.observe(this, { shouldNavigate ->
            if(shouldNavigate) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                startActivity(intent)
                finish()
            }
        })

        firstSearchViewModel.toastMessage.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                hideOverlay()
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }

    private fun checkPermissions() {
        if(!permissionGranted(this, permissions)) {
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
                            val uri = Uri.fromParts("package", this.packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }

                    snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.accent_blue))
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
            if (isOnline(this)) {
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

                    val client = LocationServices.getSettingsClient(this)
                    val task: Task<LocationSettingsResponse> =
                        client.checkLocationSettings(builder.build())

                    task.addOnFailureListener { e ->
                        if (e is ResolvableApiException) {
                            try {
                                e.startResolutionForResult(this, LOCATION_SETTINGS_REQUEST)
                            } catch (sendEx: IntentSender.SendIntentException) {}
                        }
                    }

                    fusedClient.requestLocationUpdates(mRequest, mCallback, Looper.getMainLooper())
                }
            } else {
                Toast.makeText(this, this.resources.getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOverlay() {
        binding.overlay.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        binding.overlay.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.location.visibility = View.INVISIBLE
        binding.locationProgressBar.visibility = View.VISIBLE
        binding.overlay.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.location.visibility = View.VISIBLE
        binding.locationProgressBar.visibility = View.GONE
        binding.overlay.visibility = View.GONE
    }

    private fun manageLocationData(location: Location) {
        showProgressBar()

        val lat = location.latitude
        val lon = location.longitude

        val gcd = Geocoder(this, Locale.ENGLISH)
        val addresses: List<Address>? = gcd.getFromLocation(lat, lon, 1)
        addresses?.let {
            if (addresses.isNotEmpty()) {
                firstSearchViewModel.getCityDataToAddByCoord(lat, lon, addresses[0].locality, addresses[0].countryCode)
            }
        }

    }
}