package com.example.weatherapp.model.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemHourlyBinding
import com.example.weatherapp.model.response.onecall.Hourly
import com.example.weatherapp.utils.preferencesManager
import com.example.weatherapp.utils.timeFormat


class HourlyForecastAdapter: RecyclerView.Adapter<HourlyForecastAdapter.Holder>() {

    private var hourlyDataList = listOf<Hourly>()

    inner class Holder(private val binding: ItemHourlyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(hourly: Hourly, context: Context) {
            binding.hourly = hourly

            if(hourlyDataList.indexOf(hourly) != 0){
                binding.hourlyTime.text = timeFormat(hourly.dt, preferencesManager.timeZone!!)
            } else {
                binding.hourlyTime.text = context.resources.getString(R.string.now)
            }
        }
    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val binding = ItemHourlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return hourlyDataList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(hourlyDataList[position], holder.itemView.context)
    }

    fun setData(data: List<Hourly>) {
        hourlyDataList = data
        notifyDataSetChanged()
    }

}