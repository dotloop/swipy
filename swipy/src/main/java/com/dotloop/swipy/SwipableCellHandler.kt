package com.dotloop.swipy

import com.dotloop.swipy.SwipableCellHandler.Mode.Single

class SwipableCellHandler() {

    private val openHistory: MutableMap<Int, SwipeLayout> = HashMap()
    var mode = Single

    fun bindSwipableCell(swipeLayout: SwipeLayout, position: Int) {
        swipeLayout.tag = position
        swipeLayout.onSwipeListeners = (object : OnSwipeListener {
            override fun onClose(layout: SwipeLayout) {
                openHistory.remove(layout.tag as Int)
            }

            override fun onStartOpen(layout: SwipeLayout) {
                val pos = layout.tag as Int
                if (mode == Single && !openHistory.isEmpty() && !openHistory.containsKey(pos)) {
                    openHistory.keys.forEach { openHistory[it]?.close(smooth = true, notify = true) }
                }
            }

            override fun onOpen(layout: SwipeLayout) {
                openHistory[layout.tag as Int] = layout
            }
        })

        swipeLayout.simpleOnLayoutChangeListener = (object : SimpleOnLayoutChangeListener {
            override fun onLayoutChange(layout: SwipeLayout) =
                if (isOpen(layout.tag as Int)) layout.open(smooth = false, notify = false)
                else layout.close(smooth = false, notify = false)
        })

    }

    @JvmOverloads
    fun reset(position: Int? = null) {
        when (position) {
            null -> openHistory.clear()
            else -> openHistory.remove(position)
        }
    }

    fun isOpen(position: Int): Boolean {
        return openHistory.containsKey(position)
    }

    enum class Mode {
        Single, Multiple
    }
}
