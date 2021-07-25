package com.example.weatherapp.model.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.recyclerview.widget.*
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemLocationBinding
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.utils.enableSearchView
import com.example.weatherapp.utils.preferencesManager
import com.example.weatherapp.utils.widgets.CheckableImageView
import com.example.weatherapp.viewmodel.LocationListViewModel
import java.util.*


class LocationListAdapter(val locationListViewModel: LocationListViewModel, val searchView: androidx.appcompat.widget.SearchView): RecyclerView.Adapter<LocationListAdapter.Holder>() {

    private val payloadActionModeOn = "payload_action_mode_on"
    private val payloadActionModeOff = "payload_action_mode_off"
    private val payloadSelectAll = "payload_select_all"
    private val payloadUnselectAll = "payload_unselect_all"

    private lateinit var mRecyclerView: RecyclerView

    var locationsList: ArrayList<Location> = arrayListOf()

    var actionModeEnabled = false
    var allSelected = false
    var actionMode: ActionMode? = null

    var firstClickedItem: Location? = null
    var itemClicked = false

    var selectedList = arrayListOf<Location>()

    val selectedListSize = MutableLiveData(0)

    private var touchHelper: ItemTouchHelper? = null

    private val currentCity = preferencesManager.city
    private val currentCountryCode = preferencesManager.countryCode

    inner class Holder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val checkbox = binding.checkbox
        val handle = binding.dragHandle

        @SuppressLint("ClickableViewAccessibility")
        fun bind(location: Location, context: Context) {
            binding.location = location

            binding.root.background = if(location.isDay) {
                ContextCompat.getDrawable(context, R.drawable.round_corners_day)
            } else {
                ContextCompat.getDrawable(context, R.drawable.round_corners_night)
            }

            if(location.city == currentCity && location.countryCode == currentCountryCode) {
                binding.locationName.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_current_selected),
                    null
                )
            } else {
                binding.locationName.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null
                )
            }

            binding.root.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.scaleX = 0.96f
                        view.scaleY = 0.96f
                        false
                    }

                    MotionEvent.ACTION_UP -> {
                        view.scaleX = 1f
                        view.scaleY = 1f
                        false
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        view.scaleX = 1f
                        view.scaleY = 1f
                        false
                    }

                    else -> false
                }
            }

            if(actionModeEnabled) {
                binding.checkbox.visibility = View.VISIBLE
                binding.dragHandle.visibility = View.VISIBLE
                binding.checkbox.isChecked = allSelected
            } else {
                binding.checkbox.isChecked = false
                binding.checkbox.visibility = View.GONE
                binding.dragHandle.visibility = View.GONE
            }

            binding.root.setOnClickListener { view ->
                if(actionModeEnabled) {
                    clickItem(binding.checkbox, location)
                } else {
                    preferencesManager.locationId = location.id
                    preferencesManager.city = location.city
                    preferencesManager.countryCode = location.countryCode
                    preferencesManager.lat = location.lat
                    preferencesManager.lon = location.lon

                    view.findNavController().navigate(R.id.action_locationList_to_currentWeather)
                }
            }

            binding.root.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    if(!actionModeEnabled) {
                        actionModeEnabled = true

                        val callback = object : ActionMode.Callback {
                            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                mode?.menuInflater?.inflate(R.menu.menu_locations_list, menu)
                                return true
                            }

                            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                firstClickedItem = location
                                itemClicked = true
                                notifyItemRangeChanged(0, itemCount, payloadActionModeOn)

                                selectedListSize.observe(context as LifecycleOwner, {
                                    mode?.title = String.format(context.resources.getString(R.string.val_selected), it)
                                    val menuDeleteItem = menu?.findItem(R.id.delete)
                                    val menuSelectAllItem = menu?.findItem(R.id.select_all)

                                    if(it > 0) {
                                        menuDeleteItem?.icon?.mutate()?.alpha = 255
                                        menuDeleteItem?.isEnabled = true
                                    } else {
                                        menuDeleteItem?.icon?.mutate()?.alpha = 180
                                        menuDeleteItem?.isEnabled = false
                                    }

                                    if(it == itemCount) {
                                        menuSelectAllItem?.icon?.mutate()?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                            ContextCompat.getColor(context, R.color.accent_blue),
                                            BlendModeCompat.SRC_ATOP)
                                    } else {
                                        val colorControl = TypedValue()
                                        context.theme.resolveAttribute(
                                            R.attr.colorControlNormal,
                                            colorControl,
                                            true
                                        )
                                        menuSelectAllItem?.icon?.mutate()?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                            colorControl.data,
                                            BlendModeCompat.SRC_ATOP)
                                    }
                                })

                                return true
                            }

                            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                                when(item?.itemId) {
                                    R.id.select_all -> {
                                        if(selectedList.size == locationsList.size) {
                                            allSelected = false
                                            selectedList.clear()
                                            selectedListSize.postValue(0)

                                            notifyItemRangeChanged(0, itemCount, payloadUnselectAll)
                                        } else {
                                            allSelected = true
                                            selectedList.clear()
                                            selectedList.addAll(locationsList)
                                            selectedListSize.postValue(selectedList.size)

                                            notifyItemRangeChanged(0, itemCount, payloadSelectAll)
                                        }

                                    }

                                    R.id.delete -> {
                                        locationListViewModel.deleteMultiple(selectedList)

                                        allSelected = false
                                        selectedList.clear()
                                        actionMode?.finish()
                                    }
                                }

                                return true
                            }

                            override fun onDestroyActionMode(mode: ActionMode?) {
                                locationListViewModel.updateList(locationsList)

                                actionModeEnabled = false
                                allSelected = false

                                selectedList.clear()
                                selectedListSize.postValue(0)

                                searchView.animate().setDuration(300).alpha(1f).withStartAction {
                                    enableSearchView(searchView, true)
                                }

                                notifyItemRangeChanged(0, itemCount, payloadActionModeOff)
                            }

                        }

                        actionMode = (v?.context as AppCompatActivity).startActionMode(callback)
                        searchView.animate().setDuration(300).alpha(0.5f).withStartAction {
                            enableSearchView(searchView, false)
                        }
                    } else {
                        touchHelper?.startDrag(this@Holder)
                    }

                    return true
                }
            })
            
            binding.dragHandle.setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(this@Holder)
                }
                false
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)

    }

    override fun getItemCount(): Int {
        return locationsList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(locationsList[position], holder.itemView.context)
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload in payloads) {
                when(payload) {
                    payloadActionModeOn -> {
                        holder.checkbox.visibility = View.VISIBLE
                        holder.handle.visibility = View.VISIBLE
                        holder.checkbox.isChecked = allSelected

                        if(locationsList[position] == firstClickedItem && itemClicked) {
                            clickItem(holder.checkbox, locationsList[position])
                            itemClicked = false
                        }
                    }

                    payloadActionModeOff -> {
                        holder.checkbox.visibility = View.GONE
                        holder.handle.visibility = View.GONE
                        holder.checkbox.isChecked = false
                    }

                    payloadSelectAll -> {
                        holder.checkbox.isChecked = true
                    }

                    payloadUnselectAll -> {
                        holder.checkbox.isChecked = false
                    }
                }
            }
        }
    }

    private fun clickItem(checkbox: CheckableImageView, location: Location) {
        if(checkbox.isChecked) {
            checkbox.isChecked = false
            selectedList.remove(location)
        } else {
            checkbox.isChecked = true
            selectedList.add(location)
        }

        selectedListSize.postValue(selectedList.size)

    }

    fun deleteLocation(position: Int) {
        locationListViewModel.deleteLocation(locationsList[position])
    }

    fun setData(data: ArrayList<Location>) {
        if(locationsList.isEmpty()){
            mRecyclerView.alpha = 0f
            mRecyclerView.animate().setDuration(600).alpha(1f)
        }

        if(data.size > locationsList.size) {
            mRecyclerView.smoothScrollToPosition(0)
        }

        val diffCallback = LocationsDiffCallback(locationsList, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        locationsList.clear()
        locationsList.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setTouchHelper(helper: ItemTouchHelper) {
        touchHelper = helper
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    fun onViewMoved(oldPosition: Int, newPosition: Int) {
        if (oldPosition < newPosition) {
            for (i in oldPosition until newPosition) {
                Collections.swap(locationsList, i, i + 1)
                val order1: Int = locationsList[i].order
                val order2: Int = locationsList[i + 1].order
                locationsList[i].order = order2
                locationsList[i + 1].order = order1
            }
        } else {
            for (i in oldPosition downTo newPosition + 1) {
                Collections.swap(locationsList, i, i - 1)
                val order1: Int = locationsList[i].order
                val order2: Int = locationsList[i - 1].order
                locationsList[i].order = order2
                locationsList[i - 1].order = order1
            }
        }

        notifyItemMoved(oldPosition, newPosition)
    }

}