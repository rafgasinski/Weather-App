package com.example.weatherapp.utils.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.example.weatherapp.R


class ActionButton(context: Context, drawableId: Int, private val listener: ActionButtonClickListener) {

    private var pos = 0
    private var clickRegion: RectF? = null
    private var d: Drawable? = null
    private val p = Paint()

    init {
        val value = TypedValue()
        context.theme.resolveAttribute(R.attr.colorPrimary, value, true)
        p.color = value.data
        d = ContextCompat.getDrawable(context, drawableId)
    }

    fun onClick(x: Float, y: Float) : Boolean {
        if(clickRegion != null && clickRegion!!.contains(x, y)){
            listener.onClick(pos)
        } else if (clickRegion != null && !clickRegion!!.contains(x, y)) {
            listener.notHit(pos)
        }
        return true
    }

    fun onDraw(c: Canvas, rectF: RectF, pos: Int) {

        val bitmap = drawableToBitmap(d)

        val left = (rectF.left + 30f + rectF.right)/2-bitmap.width /2
        val top = (rectF.top + rectF.bottom)/2-bitmap.height /2

        c.drawBitmap(bitmap, left, top, p)

        this.pos = pos
        clickRegion = RectF(left, top, left + d!!.intrinsicWidth, top + d!!.intrinsicHeight)
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