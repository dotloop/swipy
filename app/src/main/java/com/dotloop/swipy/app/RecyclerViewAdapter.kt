package com.dotloop.swipy.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dotloop.swipy.SwipableCellHandler
import com.dotloop.swipy.SwipeLayout

class RecyclerViewAdapter(
    private val dataSet: List<String>,
    private val listener: ActionListener) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    private val handler: SwipableCellHandler = SwipableCellHandler()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val swipeLayout: SwipeLayout = itemView.findViewById(R.id.swipe_layout)
        internal val titleView: TextView = itemView.findViewById(R.id.text)
        internal val archiveActionView: View = itemView.findViewById(R.id.archive)
        internal val deleteActionView: View = itemView.findViewById(R.id.delete)
        internal val phoneActionView: View = itemView.findViewById(R.id.phone)
        internal val emailActionView: View = itemView.findViewById(R.id.email)
        internal val txtmessageActionView: View = itemView.findViewById(R.id.txtmessage)

        init {
            swipeLayout.addDrag(SwipeLayout.DragEdge.LEFT_EDGE, swipeLayout.findViewById(R.id.swipe_left_action))
            swipeLayout.addDrag(SwipeLayout.DragEdge.RIGHT_EDGE, swipeLayout.findViewById(R.id.swipe_right_action))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder, position: Int) {
        handler.bindSwipableCell(viewHolder.swipeLayout, position)
        viewHolder.titleView.text = dataSet[position]
        viewHolder.archiveActionView.setOnClickListener { listener.onArchiveClicked() }
        viewHolder.deleteActionView.setOnClickListener { listener.onDeleteClicked() }
        viewHolder.emailActionView.setOnClickListener { listener.onEmailClicked() }
        viewHolder.phoneActionView.setOnClickListener { listener.onPhoneClicked() }
        viewHolder.txtmessageActionView.setOnClickListener { listener.onTxtMessageClicked() }

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}
