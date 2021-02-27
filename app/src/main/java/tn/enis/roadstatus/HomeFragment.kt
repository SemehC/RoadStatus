package tn.enis.roadstatus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.marginBottom
import tn.enis.roadstatus.db.Converters
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus


class HomeFragment : Fragment(R.layout.fragment_home) {

    var lin_layout: LinearLayout?=null
    var roads:List<RoadStatus>?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lin_layout = view.findViewById(R.id.lin_layout)

        roads = DatabaseHandler().getAllRoadStatus(view.context)

        roads?.let { addDataToView(it) }

        val rmbt = view.findViewById<Button>(R.id.rm_data2)
        rmbt.setOnClickListener {
            confirmDeleteAll()
        }

        val bt = view.findViewById<Button>(R.id.start_scan_bt2)
        bt.setOnClickListener {
            val intent = Intent(view.context, SamplingActivity::class.java)
            startActivity(intent)

        }

    }

    private fun addDataToView(roads:List<RoadStatus>){

        roads.forEach { h->

            var txt = TextView(view?.context)
            txt.text=h.file_name



            txt.setOnClickListener {
                clickedOnItem(h.id)
            }
            lin_layout?.addView(txt)
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
            removeAllData()
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

    private fun removeAllData(){
        DatabaseHandler().removeAllData(view?.context!!)
        Toast.makeText(view?.context,"Remove all entries",Toast.LENGTH_SHORT).show()
        roads = DatabaseHandler().getAllRoadStatus(view?.context!!)
        roads?.let { addDataToView(it) }
    }

}