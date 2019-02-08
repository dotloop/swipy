package com.dotloop.sweepy.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sweepy_demo.*
import java.util.*

class SweepyDemoActivity : AppCompatActivity(), ActionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sweepy_demo)

        // Layout Managers:
        recycler_view.layoutManager = LinearLayoutManager(this)

        // Adapter:
        val dataSet = Arrays.asList("Dan", "Eric", "Mike", "Elodie")
        val adapter = RecyclerViewAdapter(dataSet, this)
        recycler_view.adapter = adapter
    }

    override fun onDeleteClicked() {
        showClickFeedback("Delete clicked")
    }

    override fun onArchiveClicked() {
        showClickFeedback("Archived clicked")
    }

    override fun onEmailClicked() {
        showClickFeedback("Email clicked")
    }

    override fun onPhoneClicked() {
        showClickFeedback("Phone clicked")
    }

    override fun onTxtMessageClicked() {
        showClickFeedback("Txt Message clicked")
    }

    private fun showClickFeedback(label: String) {
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
    }
}
