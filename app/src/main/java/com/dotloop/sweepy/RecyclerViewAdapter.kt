package com.dotloop.sweepy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(
    private val dataSet: List<String>) : RecyclerView.Adapter<RecyclerViewAdapter.SimpleViewHolder>() {
    private val handler: SwipableCellHandler = SwipableCellHandler()

    class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val swipeLayout: SwipeLayout = itemView.findViewById(R.id.swipe_layout)
        internal val textViewPos: TextView = itemView.findViewById(R.id.text)

        init {
            swipeLayout.addDrag(SwipeLayout.DragEdge.LEFT_EDGE, swipeLayout.findViewById(R.id.swipe_left_action))
            swipeLayout.addDrag(SwipeLayout.DragEdge.RIGHT_EDGE, swipeLayout.findViewById(R.id.swipe_right_action))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(
        simpleViewHolder: SimpleViewHolder, position: Int) {
        handler.bindSwipableCell(simpleViewHolder.swipeLayout, position)
        simpleViewHolder.textViewPos.text = dataSet[position]
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}
