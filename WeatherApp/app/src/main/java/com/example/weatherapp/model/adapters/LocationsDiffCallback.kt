package com.example.weatherapp.model.adapters

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import com.example.weatherapp.model.db.location.Location

class LocationsDiffCallback(private val oldList: ArrayList<Location>, private val newList: ArrayList<Location>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val location = oldList[oldPosition]
        val location2 = newList[newPosition]

        return location == location2
    }

    @Nullable
    override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
        return super.getChangePayload(oldPosition, newPosition)
    }
}