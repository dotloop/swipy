package com.dotloop.sweepy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sweepy_demo.*
import java.util.*

class SweepyDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sweepy_demo)

        // Layout Managers:
        recycler_view.layoutManager = LinearLayoutManager(this)

        // Adapter:
        val dataSet = Arrays.asList("Dan", "Eric", "Mike", "Elodie")
        val adapter = RecyclerViewAdapter(dataSet)
        recycler_view.adapter = adapter
    }
}
