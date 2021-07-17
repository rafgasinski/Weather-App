package com.example.weatherapp.model.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemHourlyBinding
import com.example.weatherapp.model.response.onecall.Hourly
import com.example.weatherapp.utils.PreferencesManager
import com.example.weatherapp.utils.convertWindUnit
import com.example.weatherapp.utils.timeFormat
import com.example.weatherapp.utils.updateIcon
import kotlin.math.roundToInt

class HourlyForecastAdapter(private val hourlyForecastList: List<Hourly>): RecyclerView.Adapter<HourlyForecastAdapter.Holder>() {

    inner class Holder(private val binding: ItemHourlyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(hourly: Hourly, context: Context) {
            updateIcon(hourly.weather[0].icon, binding.hourlyIcon)

            val preferencesManager = PreferencesManager.getInstance()

            binding.hourlyTime.text = timeFormat(hourly.dt, preferencesManager.timeZone!!)
            binding.hourlyTemp.text = String.format(context.resources.getString(R.string.decimal_val_degrees), hourly.temp.roundToInt().toString())
            binding.hourlyWind.text = String.format(context.resources.getString(R.string.decimal_val_kmh), convertWindUnit(hourly.windSpeed))

            if(hourlyForecastList.indexOf(hourly) == 0){
                binding.hourlyTime.text = context.resources.getString(R.string.now)
            }
        }

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val binding = ItemHourlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return hourlyForecastList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(hourlyForecastList[position], holder.itemView.context)
    }
}