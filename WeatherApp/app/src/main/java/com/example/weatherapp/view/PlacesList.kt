package com.example.weatherapp.view

import android.R.attr
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentPlacesListBinding
import com.example.weatherapp.model.adapters.ItemMoveHelper
import com.example.weatherapp.model.adapters.PlacesListAdapter
import com.example.weatherapp.model.db.Place
import com.example.weatherapp.utils.ActionButton
import com.example.weatherapp.utils.ActionButtonClickListener
import com.example.weatherapp.viewmodel.PlacesListViewModel
import java.util.*


class PlacesList : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var placesListViewModel: PlacesListViewModel

    private lateinit var adapterLocationsList: PlacesListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesListBinding.inflate(inflater, container, false)

        placesListViewModel = ViewModelProvider(requireActivity()).get(PlacesListViewModel::class.java)

        placesListViewModel.updateList()

        adapterLocationsList = PlacesListAdapter(placesListViewModel, binding.searchView)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(PlacesListDirections.actionPlacesListToCurrentWeather())
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(PlacesListDirections.actionPlacesListToCurrentWeather())
        }

        if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            } else {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }

            }
        }

        placesListViewModel.allPlacesLiveData.observe(viewLifecycleOwner, { placesList ->
            adapterLocationsList.setData(placesList as ArrayList<Place>)
        })

        object : ItemMoveHelper(adapterLocationsList, binding.placesRecyclerView, binding.searchView, 180) {
            override fun instantiateActionButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<ActionButton>
            ) {
                buffer.add(
                    ActionButton(requireContext(), R.drawable.ic_delete_row,
                        object : ActionButtonClickListener {
                            override fun onClick(pos: Int) {
                                adapterLocationsList.deleteItem(pos)
                            }

                        })
                )
            }
        }

        binding.placesRecyclerView.apply {
            adapter = adapterLocationsList
            alpha = 0f
            animate().setDuration(600).alpha(1f)
        }

        binding.searchView.apply {
            val searchText = this.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
            searchText.typeface = ResourcesCompat.getFont(requireContext(), R.font.metropolis_regular)

            val searchClose = this.findViewById(androidx.appcompat.R.id.search_close_btn) as ImageView
            searchClose.setImageResource(R.drawable.ic_clear_search)

            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(textInput: String?): Boolean {
                        textInput?.let {
                            val motionEvent = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_UP,
                                0.0f,
                                0.0f,
                                0
                            )
                            this@apply.dispatchTouchEvent(motionEvent)

                            placesListViewModel.getCityDataAdd(textInput)
                        }
                        return false
                    }

                    override fun onQueryTextChange(textInput: String?): Boolean {
                        return false
                    }
                }
            )
        }

        placesListViewModel.toastMessage.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root

    }

}
