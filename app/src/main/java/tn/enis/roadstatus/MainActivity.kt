package tn.enis.roadstatus

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Utilities


class MainActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bt = findViewById<Button>(R.id.start_scan_bt)
        bt.setOnClickListener {
            val intent = Intent(this, SamplingActivity::class.java)
            startActivity(intent)
        }
        requestPermissions()
    }
    private fun requestPermissions()
    {
        if(Utilities.hasLocationPermissions(this) && Utilities.hasStoragePermissions(this))
        {
            return
        }

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                        Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else
            {
                EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                        Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }



    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms))
        {
            AppSettingsDialog.Builder(this).build().show()
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