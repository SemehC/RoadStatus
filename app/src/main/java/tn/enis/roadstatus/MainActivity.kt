package tn.enis.roadstatus

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //Testing room db

        val r =RoadStatus()
        var roads:List<RoadStatus>?=null
        r.date=22
        r.avg_speed=22f
        r.distance=22f
        r.file_name="test"
        r.total_time=22

        val bt = findViewById<Button>(R.id.start_scan_bt)
        bt.setOnClickListener {
            val intent = Intent(this, SamplingActivity::class.java)
            startActivity(intent)
            finish()
        }
        requestPermissions()
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