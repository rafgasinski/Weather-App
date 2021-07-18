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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemPlaceBinding
import com.example.weatherapp.model.db.Place
import com.example.weatherapp.utils.CheckableImageView
import com.example.weatherapp.utils.PreferencesManager
import com.example.weatherapp.viewmodel.PlacesListViewModel
import java.util.*


class PlacesListAdapter(var placesListViewModel: PlacesListViewModel, var searchView: androidx.appcompat.widget.SearchView): RecyclerView.Adapter<PlacesListAdapter.Holder>() {

    private lateinit var mRecyclerView: RecyclerView

    var placesList: ArrayList<Place> = arrayListOf()

    var actionModeEnabled = false
    var allSelected = false
    var actionMode: ActionMode? = null

    var firstClickedItem: Place? = null
    var itemClicked = false
    var actionModeFinished = false

    var selectedList = arrayListOf<Place>()

    val selectedListSize = MutableLiveData(0)

    private var touchHelper: ItemTouchHelper? = null

    private val preferencesManager = PreferencesManager.getInstance()

    private val currentCity = preferencesManager.city
    private val currentCountryCode = preferencesManager.countryCode

    inner class Holder(private val binding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(place: Place, context: Context) {
            binding.placeName.text = place.name
            binding.temps.text = String.format(context.resources.getString(R.string.decimal_val_temps), place.tempMax, place.tempMin)
            binding.placeCurrentTemp.text = String.format(context.resources.getString(R.string.decimal_val_degree), place.currentTemp)

            binding.root.background =
                if (place.isDay) {
                    ContextCompat.getDrawable(context, R.drawable.round_corners_place_day)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.round_corners_place_night)
                }

            if (place.name == String.format(context.resources.getString(R.string.place_country), currentCity, currentCountryCode)) {
                binding.placeName.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_location),
                    null
                )
            } else {
                binding.placeName.setCompoundDrawablesWithIntrinsicBounds(
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

            if (actionModeEnabled) {
                binding.checkbox.visibility = View.VISIBLE
                binding.dragHandle.visibility = View.VISIBLE

                binding.checkbox.isChecked = allSelected

                if(firstClickedItem != null && itemClicked) {
                    if(place == firstClickedItem){
                        clickItem(binding.checkbox, place)
                        itemClicked = false
                    }
                }

            } else {
                binding.checkbox.isChecked = false
                binding.checkbox.visibility = View.GONE
                binding.dragHandle.visibility = View.GONE
            }

            binding.root.setOnClickListener { x ->
                if (actionModeEnabled) {
                    clickItem(binding.checkbox, place)
                } else {
                    preferencesManager.city = place.name.split(",")[0]
                    preferencesManager.countryCode = place.name.split(",")[1].trim()
                    preferencesManager.lat = place.lat.toString()
                    preferencesManager.lon = place.lon.toString()

                    x.findNavController().navigate(R.id.action_placesList_to_currentWeather)
                }
            }

            binding.root.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    if(!actionModeEnabled) {
                        actionModeEnabled = true
                        actionModeFinished = false
                        preferencesManager.isSwipeEnabled = false

                        val callback = object : ActionMode.Callback {
                            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                mode?.menuInflater?.inflate(R.menu.menu_places_list, menu)
                                return true
                            }

                            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                firstClickedItem = place
                                itemClicked = true
                                notifyDataSetChanged()

                                selectedListSize.observe(context as LifecycleOwner, {
                                    mode?.title = String.format(context.resources.getString(R.string.decimal_selected), it)
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
                                        if(selectedList.size == placesList.size) {
                                            allSelected = false
                                            selectedList.clear()

                                            selectedListSize.postValue(0)
                                        } else {
                                            allSelected = true
                                            selectedList.clear()
                                            selectedList.addAll(placesList)

                                            selectedListSize.postValue(selectedList.size)
                                        }

                                        notifyDataSetChanged()
                                    }

                                    R.id.delete -> {
                                        val toDeleteListId = arrayListOf<Int>()
                                        selectedList.forEach {
                                            toDeleteListId.add(it.id)
                                        }

                                        placesListViewModel.deleteMultiple(toDeleteListId)

                                        allSelected = false
                                        selectedList.clear()
                                        actionMode?.finish()
                                    }
                                }

                                return true
                            }

                            override fun onDestroyActionMode(mode: ActionMode?) {
                                actionModeEnabled = false
                                actionModeFinished = true
                                allSelected = false

                                selectedList.clear()

                                selectedListSize.postValue(selectedList.size)
                                preferencesManager.isSwipeEnabled = true

                                searchView.animate().setDuration(300).alpha(1f).withStartAction {
                                    enableSearchView(searchView, true)
                                }

                                val colorControl = TypedValue()
                                context.theme.resolveAttribute(
                                    R.attr.colorControlNormal,
                                    colorControl,
                                    true
                                )

                                notifyDataSetChanged()
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
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)

    }

    override fun getItemCount(): Int {
        return placesList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(placesList[position], holder.itemView.context)
    }

    private fun clickItem(checkbox: CheckableImageView, place: Place) {
        if(checkbox.isChecked) {
            checkbox.isChecked = false
            selectedList.remove(place)
        } else {
            checkbox.isChecked = true
            selectedList.add(place)
        }

        selectedListSize.postValue(selectedList.size)

    }

    fun deleteItem(position: Int) {
        placesListViewModel.deletePlace(placesList[position])
    }

    fun setData(data: ArrayList<Place>) {
        if(placesList.isEmpty()){
            mRecyclerView.alpha = 0f
            mRecyclerView.animate().setDuration(600).alpha(1f)
        }

        if(data.size > placesList.size) {
            mRecyclerView.smoothScrollToPosition(0)
        }

        val diffCallback = PlacesDiffCallback(placesList, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        placesList.clear()
        placesList.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setTouchHelper(helper: ItemTouchHelper) {
        touchHelper = helper
    }

    private fun enableSearchView(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                enableSearchView(child, enabled)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    fun onViewMoved(oldPosition: Int, newPosition: Int) {
        if (oldPosition < newPosition) {
            for (i in oldPosition until newPosition) {
                Collections.swap(placesList, i, i + 1)
                val order1: Int = placesList[i].order
                val order2: Int = placesList[i + 1].order
                placesList[i].order = order2
                placesList[i + 1].order = order1
            }
        } else {
            for (i in oldPosition downTo newPosition + 1) {
                Collections.swap(placesList, i, i - 1)
                val order1: Int = placesList[i].order
                val order2: Int = placesList[i - 1].order
                placesList[i].order = order2
                placesList[i - 1].order = order1
            }
        }

        placesListViewModel.updateList(placesList)
        notifyItemMoved(oldPosition, newPosition)
    }

}