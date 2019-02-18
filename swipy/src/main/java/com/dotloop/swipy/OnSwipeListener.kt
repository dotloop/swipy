package com.dotloop.swipy

interface OnSwipeListener {
    fun onStartOpen(layout: SwipeLayout){}
    fun onOpen(layout: SwipeLayout){}
    fun onStartClose(layout: SwipeLayout){}
    fun onClose(layout: SwipeLayout){}
}