package tn.enis.roadstatus.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_home.*
import tn.enis.roadstatus.MainActivity
import tn.enis.roadstatus.R
import tn.enis.roadstatus.other.RoadStatusItem
import tn.enis.roadstatus.RoadStatusItemAdapter
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var roads = ArrayList<RoadStatus>()
    private val arrayList = ArrayList<RoadStatusItem>()
    lateinit var myAdapter: RoadStatusItemAdapter

    private var t: Toast? = null
    var mainActivity: MainActivity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        swiperefresh.setOnRefreshListener {
            refresh()
            swiperefresh.isRefreshing = false
        }

        refresh()
        myAdapter = RoadStatusItemAdapter(arrayList, view.context!!, mainActivity!!, this)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = myAdapter

    }

    @SuppressLint("SimpleDateFormat")
    private fun prepareData(): Boolean {
        arrayList.clear()
        roads = ArrayList(DatabaseHandler().getAllRoadStatus(view?.context!!))
        roads.forEach {
            val date = Date(it.date)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            arrayList.add(
                RoadStatusItem(
                    it.id,
                    it.label, format.format(date).toString(), it.img, it.file_name!!
                )
            )
        }

        return true
    }


    fun refresh() {
        val newData = prepareData()
        if (newData) {
            myAdapter = RoadStatusItemAdapter(arrayList, view?.context!!, mainActivity!!, this)
            recyclerView.layoutManager = LinearLayoutManager(view?.context)
            recyclerView.adapter = myAdapter
        } else {
            t?.cancel()
            t = Toast.makeText(view?.context, "Up to date", Toast.LENGTH_SHORT)
            t?.show()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }


}