package com.example.weatherapp.utils.widgets

import android.content.Context
import android.util.AttributeSet
import com.example.weatherapp.utils.capitalizeFirst


class CapitalizedTextView constructor(context: Context, attrs: AttributeSet): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    override fun setText(text: CharSequence, type: BufferType) {
        var textToCapitalize = text

        if (textToCapitalize.isNotEmpty()) {
            textToCapitalize = text.toString().capitalizeFirst
        }
        super.setText(textToCapitalize, type)
    }
}