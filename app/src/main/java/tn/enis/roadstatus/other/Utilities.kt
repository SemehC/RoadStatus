package tn.enis.roadstatus.other

import android.Manifest
import android.content.Context
import android.os.Build
import pub.devrel.easypermissions.EasyPermissions


object Utilities {

    fun hasLocationPermissions(context : Context) = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                EasyPermissions.hasPermissions(context,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        else
            {
                EasyPermissions.hasPermissions(context,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
    fun hasStoragePermissions(context : Context) = EasyPermissions.hasPermissions(context,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)

}