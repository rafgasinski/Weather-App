package com.example.weatherapp.model.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.ItemForecastBinding
import com.example.weatherapp.model.response.onecall.Daily

class DailyForecastAdapter: RecyclerView.Adapter<DailyForecastAdapter.Holder>() {

    private var dailyDataList = listOf<Daily>()

    inner class Holder(private val binding: ItemForecastBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(daily: Daily) {
            binding.daily = daily
        }
    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val binding = ItemForecastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return dailyDataList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(dailyDataList[position])

    }

    fun setData(data: List<Daily>) {
        dailyDataList = data
        notifyDataSetChanged()
    }

}