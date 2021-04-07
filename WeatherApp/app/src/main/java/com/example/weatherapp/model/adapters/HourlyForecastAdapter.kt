package com.example.weatherapp.model.adapters

import android.content.SharedPreferences
import com.example.weatherapp.viewmodel.ForecastViewModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Constants
import com.example.weatherapp.R
import com.example.weatherapp.model.response.Hourly
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt

class HourlyForecastAdapter(var hourlyForecastList: List<Hourly>?, var forecastViewModel: ForecastViewModel): RecyclerView.Adapter<HourlyForecastAdapter.Holder>() {

    class Holder(val view: View): RecyclerView.ViewHolder(view) {
        val textViewTime= view.findViewById<TextView>(R.id.hourly_time)
        val textViewTemp= view.findViewById<TextView>(R.id.hourly_temp)
        val imageViewIcon= view.findViewById<ImageView>(R.id.hourly_icon)
        val textViewWind= view.findViewById<TextView>(R.id.hourly_wind)

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val sharedPref: SharedPreferences = parent.context.getSharedPreferences(Constants.SH_ACCESSIBILITY_KEY, Constants.PRIVATE_MODE)

        var view = LayoutInflater.from(parent.context).inflate(R.layout.one_column_hourly,parent, false) as View

        if(sharedPref.getBoolean(Constants.SH_ACCESSIBILITY_KEY, false)){
            view = LayoutInflater.from(parent.context).inflate(R.layout.one_column_hourly_accessibility,parent, false)
        }

        return Holder(view)
    }

    override fun getItemCount(): Int {
        return hourlyForecastList?.size?:0
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {

        val currentItem = hourlyForecastList?.get(position)

        val newIconCode = hourlyForecastList?.get(position)?.weather?.get(0)?.icon
                ?.replace("n","")
                ?.replace("d","")

        when(newIconCode) {
            "01" -> holder.imageViewIcon.setImageResource(R.drawable.icon_clear_sky)
            "02" -> holder.imageViewIcon.setImageResource(R.drawable.icon_few_clouds)
            "03" -> holder.imageViewIcon.setImageResource(R.drawable.icon_scattered_clouds)
            "04" -> holder.imageViewIcon.setImageResource(R.drawable.icon_broken_clouds)
            "09" -> holder.imageViewIcon.setImageResource(R.drawable.icon_shower_rain)
            "10" -> holder.imageViewIcon.setImageResource(R.drawable.icon_rain)
            "11" -> holder.imageViewIcon.setImageResource(R.drawable.icon_thunderstorm)
            "13" -> holder.imageViewIcon.setImageResource(R.drawable.icon_snow)
            "50" -> holder.imageViewIcon.setImageResource(R.drawable.icon_mist)
        }

        val sdf = java.text.SimpleDateFormat("HH:mm")
        val hour = hourlyForecastList?.get(position)?.dt?.toLong()?.times(1000)?.let { java.util.Date(it) }

        holder.textViewTime.text = sdf.format(hour)
        holder.textViewTemp.text = "${hourlyForecastList?.get(position)?.temp?.roundToInt()}°C"

        val decimalWindSpeed = (hourlyForecastList?.get(position)?.windSpeed)?.times(3.6)?.let { BigDecimal(it).setScale(1, RoundingMode.HALF_EVEN) }
        val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
        df.isDecimalSeparatorAlwaysShown = false
        holder.textViewWind.text = "${df.format(decimalWindSpeed)} km/h"

        if(position == 0){
            holder.textViewTime.text = "Now"
        }
    }
}