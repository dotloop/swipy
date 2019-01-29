package com.dotloop.sweepy

import com.dotloop.sweepy.SwipableCellHandler.Mode.Single
import java.util.*

class SwipableCellHandler {

    private val openPositions: MutableSet<Int> = HashSet()
    private var mode = Single


    fun bindSwipableCell(swipeLayout: SwipeLayout, position: Int) {
        swipeLayout.tag = position
        swipeLayout.onSwipeListeners = (object : OnSwipeListener {
            override fun onClose(layout: SwipeLayout) {
                openPositions.remove(layout.tag as Int)
            }

            override fun onStartOpen(layout: SwipeLayout) {
                val position = layout.tag as Int
                /**if (mode == Attributes.Mode.Single &&
                 * !openPositions.isEmpty() &&
                 * !openPositions.contains(position))
                 * adapter.notifyItemChanged(position); */
            }

            override fun onOpen(layout: SwipeLayout) {
                openPositions.add(layout.tag as Int)
            }
        })

        swipeLayout.simpleOnLayoutChangeListenerListener = (object : SimpleOnLayoutChangeListener {
            override fun onLayoutChange(layout: SwipeLayout) =
                if (isOpen(layout.tag as Int)) layout.open(false, false)
                else layout.close(false, false)
        })

    }

    @JvmOverloads
    fun reset(position: Int? = null) {
        when (position) {
            null -> openPositions.clear()
            else -> openPositions.remove(position)
        }
    }

    fun isOpen(position: Int): Boolean {
        return openPositions.contains(position)
    }

    fun setMode(mode: Mode) {
        this.mode = mode
    }

    enum class Mode {
        Single, Multiple
    }
}
