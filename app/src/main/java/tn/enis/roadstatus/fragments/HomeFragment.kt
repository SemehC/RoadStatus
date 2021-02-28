package tn.enis.roadstatus.fragments

import android.media.Image
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tn.enis.roadstatus.R
import tn.enis.roadstatus.RoadStatusItem
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swiperefresh.setOnRefreshListener {
            prepareData()
        }
        roads = ArrayList(DatabaseHandler().getAllRoadStatus(view?.context!!))


        if(roads!!.size==0){
            Toast.makeText(view?.context,"No items yet",Toast.LENGTH_SHORT).show()
        }else{
            prepareData()
            myAdapter = RoadStatusItemAdapter(arrayList,view?.context!!)
            recyclerView.layoutManager = LinearLayoutManager(view?.context)
            recyclerView.adapter = myAdapter
        }

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