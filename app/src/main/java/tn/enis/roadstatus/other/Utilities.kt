package tn.enis.roadstatus.other

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
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
    fun hasCameraPermissions(context : Context) = EasyPermissions.hasPermissions(context,Manifest.permission.CAMERA)
    fun hasMicPermission(context : Context) = EasyPermissions.hasPermissions(context,Manifest.permission.RECORD_AUDIO)
    fun hasInternetPermission(context : Context) = EasyPermissions.hasPermissions(context,Manifest.permission.INTERNET)
    fun hasAllPermissions(context: Context) = hasStoragePermissions(context)&&hasInternetPermission(context) && hasCameraPermissions(context) && hasLocationPermissions(context) && hasMicPermission(context)




    fun calculateTotalDistance(polyline: PolylineOptions):Float{
        val r:FloatArray= FloatArray(1)
        var res:Float=0f
        for(i in 0 until polyline?.points?.size!!-1){
            val pos1 = LatLng(polyline?.points?.get(i)?.latitude!!,polyline?.points?.get(i)?.longitude!!)
            val pos2 = LatLng(polyline?.points?.get(i+1)?.latitude!!,polyline?.points?.get(i+1)?.longitude!!)
            Location.distanceBetween(pos1.latitude,pos1.longitude,pos2.latitude,pos2.longitude,r)
            res+=r[0]
        }
        return res

    }


}