package com.example.weatherapp.view

import android.content.ContentValues.TAG
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.Constants
import com.example.weatherapp.model.adapters.PlacesListAdapter
import com.example.weatherapp.model.db.Search
import com.example.weatherapp.model.db.SwipeToDelete
import com.example.weatherapp.viewmodel.PlacesListViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.android.synthetic.main.fragment_places_list.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PlacesList.newInstance] factory method to
 * create an instance of this fragment.
 */
class PlacesList : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var placesListViewModel: PlacesListViewModel
    lateinit var viewManager: RecyclerView.LayoutManager
    lateinit var adapterPlacesList: PlacesListAdapter

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
        return inflater.inflate(R.layout.fragment_places_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back.setOnClickListener { x -> x.findNavController().navigate(R.id.action_placesList_to_currentWeather) }

        placesListViewModel = ViewModelProvider(requireActivity()).get(PlacesListViewModel::class.java)
        viewManager = LinearLayoutManager(requireContext())

        adapterPlacesList = PlacesListAdapter(placesListViewModel.allPlaces, placesListViewModel )

        placesList_recycler_view.apply {
            adapter = adapterPlacesList
            layoutManager = viewManager
        }

        placesListViewModel.allPlaces.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            adapterPlacesList.notifyDataSetChanged()
        })

        var itemTouchHelper = ItemTouchHelper(SwipeToDelete(adapterPlacesList))
        itemTouchHelper.attachToRecyclerView(placesList_recycler_view)

        /**
         * Setup autocomplete fragment, Places API
         * */
        val apiKey = Constants.PLACES_API_KEY

        if (!Places.isInitialized()) {
            activity?.let { Places.initialize(it, apiKey) }
        }

        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment

        autocompleteFragment.view?.setBackgroundColor(4292335575.toInt())
        autocompleteFragment.setHint("")

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ADDRESS,Place.Field.LAT_LNG, Place.Field.ID, Place.Field.NAME))

        autocompleteFragment.setTypeFilter(TypeFilter.CITIES)

        /**
         * Changing SharedPreferences keys values and setting
         * how autocomplete fragmen will behave
         * */
        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                var address = place.address?.removePrefix("${place.name}, ")
                val placeCoords = place.latLng

                val placeToAdd = place.id?.let { place.name?.let { it1 ->
                    placeCoords?.latitude?.let { it2 -> Search(0, it, it1, address!!, it2, placeCoords.longitude) } } }

                if(placesListViewModel.allPlaces.value?.isEmpty()!!){
                    if (placeToAdd != null) {
                        placesListViewModel.addPlace(placeToAdd)

                        val sharedPref: SharedPreferences? = activity?.getSharedPreferences(Constants.SH_LAST_LAT_LON_KEY, Constants.PRIVATE_MODE)
                        val editor = sharedPref?.edit()
                        editor?.putString(Constants.SH_LAST_LAT_LON_KEY, "${placeCoords?.latitude},${placeCoords?.longitude}")
                        editor?.putString(Constants.SH_LAST_CITY_KEY, "${place.name}")
                        editor?.putString(Constants.SH_LAST_PLACE_ID_KEY, "${place.id}")
                        editor?.commit()

                        findNavController().navigate(R.id.action_placesList_to_currentWeather)
                    }
                }
                else{
                    var placeNotAdded: Boolean = true

                    placesListViewModel.allPlaces.value?.forEach lit@{
                        if(it.placeId == place.id){
                            placeNotAdded = false
                            return@lit
                        }
                    }
                    if (placeToAdd != null && placeNotAdded) {
                        placesListViewModel.addPlace(placeToAdd)

                        val sharedPref: SharedPreferences? = activity?.getSharedPreferences(Constants.SH_LAST_LAT_LON_KEY, Constants.PRIVATE_MODE)
                        val editor = sharedPref?.edit()
                        editor?.putString(Constants.SH_LAST_LAT_LON_KEY, "${placeCoords?.latitude},${placeCoords?.longitude}")
                        editor?.putString(Constants.SH_LAST_CITY_KEY, "${place.name}")
                        editor?.putString(Constants.SH_LAST_PLACE_ID_KEY, "${place.id}")
                        editor?.commit()

                        findNavController().navigate(R.id.action_placesList_to_currentWeather)
                    }
                    else{
                        Toast.makeText(requireContext(), "Place already listed.", Toast.LENGTH_SHORT).show();
                    }
                }
                Log.i(TAG, "Place: ${place.address}, ${place.id}")
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }

        })

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PlacesList.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PlacesList().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
