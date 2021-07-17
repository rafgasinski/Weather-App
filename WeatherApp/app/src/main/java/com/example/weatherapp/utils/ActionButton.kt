package com.example.weatherapp.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.example.weatherapp.R


class ActionButton(private val context: Context, private val drawableId: Int, private val listener: ActionButtonClickListener) {

    private var pos = 0
    private var clickRegion: RectF? = null

    fun onClick(x: Float, y: Float) : Boolean {
        if(clickRegion != null && clickRegion!!.contains(x, y)){
            listener.onClick(pos)
        }
        return true
    }

    fun onDraw(c: Canvas, rectF: RectF, pos: Int) {

        val p = Paint()
        val value = TypedValue()
        context.theme.resolveAttribute(R.attr.colorPrimaryDark, value, true)
        p.color = value.data

        c.drawRect(rectF, p)

        val d = ContextCompat.getDrawable(context, drawableId)
        val bitmap = drawableToBitmap(d)
        c.drawBitmap(bitmap, (rectF.left + 26f + rectF.right)/2-bitmap.width /2, (rectF.top + rectF.bottom)/2-bitmap.height /2, p)

        this.pos = pos
        clickRegion = rectF
    }

    private fun drawableToBitmap(d: Drawable?) : Bitmap {
        if (d is BitmapDrawable) {
            return d.bitmap
        }

        val bitmap = Bitmap.createBitmap(d!!.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)

        return bitmap
    }

}