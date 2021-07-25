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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.utils.widgets.ActionButton
import com.example.weatherapp.utils.widgets.ActionButtonClickListener
import java.util.*
import kotlin.collections.ArrayList


@SuppressLint("ClickableViewAccessibility")
class ItemMoveHelper(private val recyclerView: RecyclerView, private val adapter: LocationsListAdapter) : ItemTouchHelper.Callback(){

    private lateinit var deleteButton: ActionButton
    private lateinit var gestureDetector: GestureDetector
    private var swipePosition = -1
    private var swipeThreshold = 0.5f
    private val buttonBuffer: MutableMap<Int, MutableList<ActionButton>>
    private lateinit var removerQueue: LinkedList<Int>

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            deleteButton.onClick(e!!.x, e.y)
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

        if(event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_UP
            || event.action == MotionEvent.ACTION_CANCEL) {
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

    fun startRecoverItem() {
        removerQueue.add(swipePosition)
        swipePosition = -1
        recoverItem()
    }

    @Synchronized
    private fun recoverItem() {
        while(removerQueue.isNotEmpty()) {
            val pos = removerQueue.poll()!!.toInt()

            if(pos > -1) {
                adapter.notifyItemChanged(pos)
            }
        }
    }

    init {
        gestureDetector = GestureDetector(recyclerView.context, gestureListener)
        recyclerView.setOnTouchListener(onTouchListener)
        buttonBuffer = HashMap()
        removerQueue = IntLinkedList()
        deleteButton =  ActionButton(recyclerView.context, R.drawable.ic_delete_row,
            object : ActionButtonClickListener {
                override fun onClick(pos: Int) {
                    adapter.deleteLocation(pos)
                }

                override fun notHit(pos: Int) {
                    startRecoverItem()
                }
            })

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
        adapter.onViewMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return !adapter.actionModeEnabled
    }

    @Suppress("DEPRECATION")
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.bindingAdapterPosition

        if(swipePosition != pos) {
            removerQueue.add(swipePosition)
        }

        swipePosition = pos

        buttonBuffer.clear()
        swipeThreshold = 0.5f * buttonWidth.toFloat()
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
        return 0.15f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5f * defaultValue
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
        val pos = viewHolder.bindingAdapterPosition
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
                    buffer.add(deleteButton)
                    buttonBuffer[pos] = buffer
                } else {
                    buffer = buttonBuffer[pos]!!
                }

                translationX = dX * buffer.size.toFloat() * buttonWidth.toFloat() / itemView.width * 1.1f
                drawButton(c, itemView, translationX, pos)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }

    private fun drawButton(c: Canvas, itemView: View, translationX: Float, pos: Int) {
        val right = itemView.right.toFloat()
        val mButtonWidth = -1 * translationX
        val left = right - mButtonWidth
        deleteButton.onDraw(c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()), pos)
    }

    companion object {
        private const val buttonWidth = 180
    }

}