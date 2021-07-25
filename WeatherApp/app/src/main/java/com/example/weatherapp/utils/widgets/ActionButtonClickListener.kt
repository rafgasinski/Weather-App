package com.example.weatherapp.utils.widgets

interface ActionButtonClickListener {
    fun onClick(pos: Int)

    fun notHit(pos: Int)
}