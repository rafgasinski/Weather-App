package com.example.weatherapp.model.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemForecastBinding
import com.example.weatherapp.model.response.onecall.Daily
import com.example.weatherapp.utils.*
import kotlin.math.roundToInt

class DailyForecastAdapter(private val dailyForecastList: List<Daily>): RecyclerView.Adapter<DailyForecastAdapter.Holder>() {

    inner class Holder(private val binding: ItemForecastBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(daily: Daily, context: Context) {
            updateIcon(daily.weather[0].icon, binding.forecastIcon)

            val tempDay = daily.temp.day.roundToInt()
            val tempNight = daily.temp.night.roundToInt()

            binding.weatherDescription.text = daily.weather[0].description.capitalizeFirst
            binding.dayWeek.text = getDayOfWeek(daily.dt)
            binding.temps.text = String.format(context.resources.getString(R.string.decimal_val_temps), tempDay, tempNight)
        }

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val binding = ItemForecastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return dailyForecastList.size
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(dailyForecastList[position], holder.itemView.context)

    }
}