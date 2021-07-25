package com.example.weatherapp.utils.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.Checkable

class CheckableImageView constructor(context: Context, attrs: AttributeSet): androidx.appcompat.widget.AppCompatImageView(context, attrs), Checkable {

    private var checked = false
    private val checkedStateList = intArrayOf(android.R.attr.state_checked)

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) mergeDrawableStates(drawableState, checkedStateList)
        return drawableState
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked != checked) {
            this.checked = checked
            refreshDrawableState()
        }
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !checked
    }

    override fun setOnClickListener(l: OnClickListener?) {
        val onClickListener = OnClickListener { v ->
            toggle()
            l!!.onClick(v)
        }
        super.setOnClickListener(onClickListener)
    }

}