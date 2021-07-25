package com.example.weatherapp.view

import android.content.Intent
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.weatherapp.FirstSearch
import com.example.weatherapp.databinding.FragmentLocationListBinding
import com.example.weatherapp.model.adapters.ItemMoveHelper
import com.example.weatherapp.model.adapters.LocationListAdapter
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.utils.*
import com.example.weatherapp.viewmodel.LocationListViewModel
import java.util.*


class LocationList: Fragment() {

    private var _binding: FragmentLocationListBinding? = null
    private val binding get() = _binding!!

    private val locationListViewModel: LocationListViewModel by activityViewModels()

    private lateinit var adapterLocationList: LocationListAdapter

    private lateinit var networkConnection: NetworkConnectionListener

    private val handler = Handler(Looper.getMainLooper())

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationListBinding.inflate(inflater, container, false)

        adapterLocationList = LocationListAdapter(locationListViewModel, binding.searchView)

        networkConnection = NetworkConnectionListener(requireContext())
        networkConnection.observe(viewLifecycleOwner, { connected ->
            if(connected) {
                handler.postDelayed({
                    locationListViewModel.updateLocationsData()
                }, 300)
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.locationsRecyclerView.apply {
            adapter = adapterLocationList
        }

        adjustTheme(requireContext(), requireActivity())

        locationListViewModel.allLocationsLiveData.observe(viewLifecycleOwner, { locationList ->
            if(!locationList.map{ "${it.city}, ${it.countryCode}" }.contains("${preferencesManager.city}, ${preferencesManager.countryCode}") && locationList.isNotEmpty()){
                val location = locationList.first()
                preferencesManager.locationId = location.id
                preferencesManager.city = location.city
                preferencesManager.countryCode = location.countryCode
                preferencesManager.lat = location.lat
                preferencesManager.lon = location.lon
            } else if(locationList.isEmpty()) {
                preferencesManager.city = ""
                preferencesManager.countryCode = ""
                preferencesManager.lat = 0.0
                preferencesManager.lon = 0.0

                val intent = Intent(requireContext(), FirstSearch::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                startActivity(intent)
                activity?.finish()
            }

            adapterLocationList.setData(locationList as ArrayList<Location>)
        })

        val itemMoveHelper = ItemMoveHelper(binding.locationsRecyclerView, adapterLocationList)

        binding.searchView.apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(textInput: String?): Boolean {
                        textInput?.let {
                            itemMoveHelper.startRecoverItem()
                            locationListViewModel.getCityDataAdd(textInput)
                        }
                        return false
                    }

                    override fun onQueryTextChange(textInput: String?): Boolean {
                        return false
                    }
                }
            )
        }

        locationListViewModel.toastMessage.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root

    }

}
