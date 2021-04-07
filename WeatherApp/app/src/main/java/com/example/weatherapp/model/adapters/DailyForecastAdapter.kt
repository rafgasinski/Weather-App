package com.example.weatherapp.model.adapters

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Constants
import com.example.weatherapp.R
import com.example.weatherapp.model.response.Daily
import com.example.weatherapp.viewmodel.ForecastViewModel
import kotlin.math.roundToInt

class DailyForecastAdapter(var dailyForecastList: List<Daily>?, var forecastViewModel: ForecastViewModel): RecyclerView.Adapter<DailyForecastAdapter.Holder>() {

    class Holder(val view: View): RecyclerView.ViewHolder(view) {
        val imageViewIcon= view.findViewById<ImageView>(R.id.forecast_icon)
        val textViewDayOfWeek= view.findViewById<TextView>(R.id.forecast_dayOfWeek)
        val textViewDescription= view.findViewById<TextView>(R.id.forecast_weatherDescription)
        val textView_tempMinMax= view.findViewById<TextView>(R.id.forecast_tempMinMax)

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val sharedPref: SharedPreferences = parent.context.getSharedPreferences(Constants.SH_ACCESSIBILITY_KEY, Constants.PRIVATE_MODE)

        var view = LayoutInflater.from(parent.context).inflate(R.layout.one_row_forecast,parent, false) as View

        if(sharedPref.getBoolean(Constants.SH_ACCESSIBILITY_KEY, false)){
            view = LayoutInflater.from(parent.context).inflate(R.layout.one_row_forecast_accessibility,parent, false)
        }

        return Holder(view)
    }

    override fun getItemCount(): Int {
        return dailyForecastList?.size?:0
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {

        //val currentItem = dailyForecastList?.get(position)

        val newIconCode = dailyForecastList?.get(position)?.weather?.get(0)?.icon
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

        val sdf = java.text.SimpleDateFormat("E")
        val dayOfWeek = dailyForecastList?.get(position)?.dt?.toLong()?.times(1000)?.let { java.util.Date(it) }

        val description = dailyForecastList?.get(position)?.weather?.get(0)?.description
        val textDayOfWeek = sdf.format(dayOfWeek)
        if (description != null) {
            holder.textViewDescription.text = description.get(0).toUpperCase()+description.substring(1)
            holder.textViewDayOfWeek.text = textDayOfWeek.get(0).toUpperCase()+textDayOfWeek.substring(1)

        }

        holder.textView_tempMinMax.text = "${dailyForecastList?.get(position)?.temp?.day?.roundToInt()}° / ${dailyForecastList?.get(position)?.temp?.night?.roundToInt()}°"

    }
}