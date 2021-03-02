package tn.enis.roadstatus.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
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


    var roads:ArrayList<RoadStatus>?=null
    val arrayList = ArrayList<RoadStatusItem>()
    lateinit var myAdapter: RoadStatusItemAdapter

    var mainActivity:MainActivity?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swiperefresh.setOnRefreshListener {
            prepareData()
        }
        roads = ArrayList(DatabaseHandler().getAllRoadStatus(view?.context!!))



        prepareData()
        myAdapter = RoadStatusItemAdapter(arrayList,view?.context!!,mainActivity!!)
        recyclerView.layoutManager = LinearLayoutManager(view?.context)
        recyclerView.adapter = myAdapter


    }

    private fun prepareData(){
        arrayList.clear()
        roads = ArrayList(DatabaseHandler().getAllRoadStatus(view?.context!!))
        roads!!.forEach {
            val date = Date(it.date)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            arrayList.add(RoadStatusItem(it.id,it.label!!,format.format(date).toString(),it.img,it.file_name!!))
        }
        swiperefresh.isRefreshing=false
    }



    override fun onResume() {
        super.onResume()
        prepareData()
    }



}