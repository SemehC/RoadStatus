package tn.enis.roadstatus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_home.*
import tn.enis.roadstatus.db.Converters
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : Fragment(R.layout.fragment_home) {


    var roads:List<RoadStatus>?=null
    val arrayList = ArrayList<RoadStatusItem>()



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        swiperefresh.setOnRefreshListener {
            Toast.makeText(view?.context,"Refreshed",Toast.LENGTH_SHORT).show()
            loadData()
            swiperefresh.isRefreshing=false
        }


        loadData()


    }

    private fun loadData(){
        roads = DatabaseHandler().getAllRoadStatus(view?.context!!)

        prepareData(roads!!)


        val myAdapter = RoadStatusItemAdapter(arrayList,view?.context!!)

        recyclerView.layoutManager = LinearLayoutManager(view?.context)
        recyclerView.adapter = myAdapter
    }

    private fun prepareData(roads:List<RoadStatus>){
        arrayList.clear()
        roads.forEach {
            val date = Date(it.date)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            arrayList.add(RoadStatusItem(it.id,it.label!!,format.format(date).toString(),it.img,it.file_name!!))
        }
    }



    private fun clickedOnItem(id:Int){
        Toast.makeText(view?.context,"Clicked on id : $id", Toast.LENGTH_SHORT).show()
    }


    private fun confirmDeleteAll(){
        val builder = AlertDialog.Builder(view?.context!!)

        // Set the alert dialog title
        builder.setTitle("App background color")

        // Display a message on alert dialog
        builder.setMessage("Are you want to delete all data?")

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton("YES"){dialog, which ->
            // Do something when user press the positive button

        }


        // Display a negative button on alert dialog
        builder.setNegativeButton("No"){dialog,which ->
            Toast.makeText(view?.context,"Canceled.",Toast.LENGTH_SHORT).show()
        }


        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }



}