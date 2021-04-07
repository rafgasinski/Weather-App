package com.example.weatherapp.model.adapters

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Constants
import com.example.weatherapp.R
import com.example.weatherapp.model.db.Search
import com.example.weatherapp.viewmodel.PlacesListViewModel


class PlacesListAdapter(var placesList: LiveData<List<Search>>, var placesListViewModel: PlacesListViewModel): RecyclerView.Adapter<PlacesListAdapter.Holder>() {

    class Holder(val view: View): RecyclerView.ViewHolder(view) {
        val textViewName= view.findViewById<TextView>(R.id.city)
        val textViewCountry= view.findViewById<TextView>(R.id.country)
        val choosenLocation= view.findViewById<ImageView>(R.id.choosenLocation)

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_row_places_list,parent, false) as View

        return Holder(view)
    }

    override fun getItemCount(): Int {
        return placesList.value?.size?:0
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {

        val currentItem = placesList.value?.get(position)
        val sharedPref: SharedPreferences = holder.textViewName.context.getSharedPreferences(Constants.SH_LAST_LAT_LON_KEY, Constants.PRIVATE_MODE)

        holder.textViewName.text= placesList.value?.get(position)?.name
        holder.textViewCountry.text = placesList.value?.get(position)?.country
        if(sharedPref.getString(Constants.SH_LAST_PLACE_ID_KEY, "") == currentItem?.placeId){
            holder.choosenLocation.visibility = View.VISIBLE
        }
        else{
            holder.choosenLocation.visibility = View.GONE
        }

        holder.itemView.setOnClickListener{ x ->
            val editor = sharedPref.edit()
            if (currentItem != null) {
                editor.putString(Constants.SH_LAST_LAT_LON_KEY, "${currentItem.lat},${currentItem.lon}")
                editor.putString(Constants.SH_LAST_CITY_KEY, "${currentItem.name}")
                editor.putString(Constants.SH_LAST_PLACE_ID_KEY, "${currentItem.placeId}")
            }
            editor.commit()

            x.findNavController().navigate(R.id.action_placesList_to_currentWeather)
            
        }

    }

    fun deleteItem(position: Int) {
        placesList.value?.get(position)?.let { placesListViewModel.deletePlace(it) }
    }
}