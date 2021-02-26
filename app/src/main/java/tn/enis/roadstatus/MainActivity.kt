package tn.enis.roadstatus

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import tn.enis.roadstatus.db.RoadStatusDatabase
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Utilities


class MainActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {

    var lin_layout:LinearLayout?=null
    var roads:List<RoadStatus>?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        lin_layout = findViewById(R.id.lin_layout)
        //Testing room db


        roads = DatabaseHandler().getAllRoadStatus(this)


        roads?.let { addDataToView(it) }

        val rmbt = findViewById<Button>(R.id.rm_data)
        rmbt.setOnClickListener {
            confirmDeleteAll()
        }

        val bt = findViewById<Button>(R.id.start_scan_bt)
        bt.setOnClickListener {
            val intent = Intent(this, SamplingActivity::class.java)
            startActivity(intent)
            finish()
        }
        requestPermissions()
    }


    private fun addDataToView(roads:List<RoadStatus>){
        lin_layout?.removeAllViews()
        roads.forEach { h->
            val item = TextView(this)
            item.text=h.file_name
            item.setOnClickListener {
                clickedOnItem(h.id)
            }
            lin_layout?.addView(item)
        }
    }


    private fun clickedOnItem(id:Int){
        Toast.makeText(this,"Clicked on id : $id",Toast.LENGTH_SHORT).show()
    }


    private fun confirmDeleteAll(){
        val builder = AlertDialog.Builder(this@MainActivity)

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
            Toast.makeText(applicationContext,"Canceled.",Toast.LENGTH_SHORT).show()
        }


        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    private fun removeAllData(){
        DatabaseHandler().removeAllData(this)
        Toast.makeText(this,"Remove all entries",Toast.LENGTH_SHORT).show()
        roads = DatabaseHandler().getAllRoadStatus(this)
        roads?.let { addDataToView(it) }
    }

    private fun requestPermissions()
    {
        if(Utilities.hasAllPermissions(this))
        {
            return
        }

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                        Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
            }
            else
            {
                EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                        Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.RECORD_AUDIO)
            }



    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms))
        {
            AppSettingsDialog.Builder(this).setRationale("Il faut accepter tout les permissions " +
                    "pour utiliser cette application, merci de les accepter manuellement !").build().show()
        }
        else
        {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,this)
    }

}