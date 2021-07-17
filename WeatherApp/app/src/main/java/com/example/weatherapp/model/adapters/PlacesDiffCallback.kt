package com.example.weatherapp.model.adapters

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import com.example.weatherapp.model.db.Place

class PlacesDiffCallback(private val oldList: ArrayList<Place>, private val newList: ArrayList<Place>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val place = oldList[oldPosition]
        val place2 = newList[newPosition]

        return place == place2
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}