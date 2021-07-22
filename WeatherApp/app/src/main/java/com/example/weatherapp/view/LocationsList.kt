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
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentLocationsListBinding
import com.example.weatherapp.model.adapters.ItemMoveHelper
import com.example.weatherapp.model.adapters.LocationsListAdapter
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.utils.*
import com.example.weatherapp.utils.widgets.ActionButton
import com.example.weatherapp.utils.widgets.ActionButtonClickListener
import com.example.weatherapp.viewmodel.LocationsListViewModel
import java.util.*


class LocationsList: Fragment() {

    private var _binding: FragmentLocationsListBinding? = null
    private val binding get() = _binding!!

    private val locationsListViewModel: LocationsListViewModel by activityViewModels()

    private lateinit var adapterLocationsList: LocationsListAdapter

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
        _binding = FragmentLocationsListBinding.inflate(inflater, container, false)

        adapterLocationsList = LocationsListAdapter(locationsListViewModel, binding.searchView)

        networkConnection = NetworkConnectionListener(requireContext())
        networkConnection.observe(viewLifecycleOwner, { connected ->
            if(connected) {
                handler.postDelayed({
                    locationsListViewModel.updateList()
                }, 300)
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.locationsRecyclerView.apply {
            adapter = adapterLocationsList
        }

        adjustTheme(requireContext(), requireActivity())

        locationsListViewModel.allLocationsLiveData.observe(viewLifecycleOwner, { locationList ->
            if(!locationList.map{ "${it.city}, ${it.countryCode}" }.contains("${preferencesManager.city}, ${preferencesManager.countryCode}") && locationList.isNotEmpty()){
                val location = locationList.first()
                preferencesManager.locationId = location.id
                preferencesManager.city = location.city
                preferencesManager.countryCode = location.countryCode
                preferencesManager.lat = location.lat
                preferencesManager.lon = location.lon
            } else if(locationList.isEmpty()) {
                preferencesManager.locationId = 0
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

            adapterLocationsList.setData(locationList as ArrayList<Location>)
        })

        val itemMoveHelper = ItemMoveHelper(adapterLocationsList, binding.locationsRecyclerView,
            ActionButton(requireContext(), R.drawable.ic_delete_row,
            object : ActionButtonClickListener {
                override fun onClick(pos: Int) {
                    adapterLocationsList.deleteItem(pos)
                }
            })
        )

        binding.searchView.apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(textInput: String?): Boolean {
                        textInput?.let {
                            itemMoveHelper.startRecoverItem()
                            locationsListViewModel.getCityDataAdd(textInput)
                        }
                        return false
                    }

                    override fun onQueryTextChange(textInput: String?): Boolean {
                        return false
                    }
                }
            )
        }

        locationsListViewModel.toastMessage.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root

    }

}
