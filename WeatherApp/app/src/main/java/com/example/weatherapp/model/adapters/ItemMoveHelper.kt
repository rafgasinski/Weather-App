package com.example.weatherapp.model.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.ActionButton
import com.example.weatherapp.utils.PreferencesManager
import java.util.*
import kotlin.collections.ArrayList


@SuppressLint("ClickableViewAccessibility")
abstract class ItemMoveHelper(private val adapter: PlacesListAdapter, private val recyclerView: RecyclerView, private val searchView: SearchView, var buttonWidth: Int) : ItemTouchHelper.Callback(){

    private val preferencesManager = PreferencesManager.getInstance()
    private var buttonList: MutableList<ActionButton>? = null
    private lateinit var gestureDetector: GestureDetector
    private var swipePosition = -1
    private var swipeThreshold = 0.5f
    private val buttonBuffer: MutableMap<Int, MutableList<ActionButton>>
    lateinit var removerQueue: LinkedList<Int>

    abstract fun instantiateActionButton(viewHolder: RecyclerView.ViewHolder, buffer: MutableList<ActionButton>)

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            for(button in buttonList!!) {
                if(button.onClick(e!!.x, e.y)){
                    break
                }
            }
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { _, event ->
        if(swipePosition < 0) {
            return@OnTouchListener false
        }

        val point = Point(event.rawX.toInt(), event.rawY.toInt())
        val swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition)
        val swipedItem = swipeViewHolder!!.itemView
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)

        if(event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_UP) {
            if(rect.top < point.y && rect.bottom > point.y) {
                gestureDetector.onTouchEvent(event)
            } else {
                removerQueue.add(swipePosition)
                swipePosition = -1
                recoverItem()
            }
        }
        false

    }

    @Synchronized
    private fun recoverItem() {
        while(!removerQueue.isEmpty()) {
            val pos = removerQueue.poll()!!.toInt()

            if(pos > -1) {
                recyclerView.adapter!!.notifyItemChanged(pos)
            }
        }
    }

    init {
        buttonList = arrayListOf()
        gestureDetector = GestureDetector(recyclerView.context, gestureListener)
        recyclerView.setOnTouchListener(onTouchListener)
        searchView.setOnTouchListener(onTouchListener)

        buttonBuffer = HashMap()
        removerQueue = IntLinkedList()

        attachToSwipe()

    }

    class IntLinkedList : LinkedList<Int>() {
        override fun contains(element: Int): Boolean {
            return false
        }

        override fun lastIndexOf(element: Int): Int {
            return element
        }

        override fun remove(element: Int): Boolean {
            return false
        }

        override fun indexOf(element: Int): Int {
            return element
        }

        override fun add(element: Int): Boolean {
            return if(contains(element)) {
                false
            } else {
                super.add(element)
            }
        }
    }

    private fun attachToSwipe() {
        val moveHelper = this
        val itemTouchHelper = ItemTouchHelper(moveHelper)
        adapter.setTouchHelper(itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.onViewMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return preferencesManager.isSwipeEnabled
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition

        if(swipePosition != pos) {
            removerQueue.add(swipePosition)
        }

        swipePosition = pos
        if(buttonBuffer.containsKey(swipePosition)){
            buttonList = buttonBuffer[swipePosition]
        } else {
            buttonList!!.clear()
        }

        buttonBuffer.clear()
        swipeThreshold = 0.5f * buttonList!!.size.toFloat() * buttonWidth.toFloat()
        recoverItem()

        val vibrator = recyclerView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    30,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(30)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5.0f * defaultValue
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX

        val itemView = viewHolder.itemView

        if(pos < 0) {
            swipePosition = pos
            return
        }

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if(dX < 0) {
                var buffer: MutableList<ActionButton> = ArrayList()

                if(!buttonBuffer.containsKey(pos)) {
                    instantiateActionButton(viewHolder, buffer)
                    buttonBuffer[pos] = buffer
                } else {
                    buffer = buttonBuffer[pos]!!
                }

                translationX = dX * buffer.size.toFloat() * buttonWidth.toFloat() / itemView.width
                drawButton(c, itemView, buffer, translationX, pos)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }

    private fun drawButton(c: Canvas, itemView: View, buffer: MutableList<ActionButton>, translationX: Float, pos: Int) {
        var right = itemView.right.toFloat()
        val mButtonWidth = -1 * translationX / buffer.size
        for (button in buffer) {
            val left = right - mButtonWidth
            button.onDraw(c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()), pos)
            right = left
        }
    }

}
