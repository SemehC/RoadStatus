package tn.enis.roadstatus.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment

import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

import kotlinx.android.synthetic.main.fragment_road_status_item_map.*
import org.json.JSONObject
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.io.File

class RoadStatusItemMapFragment : Fragment(R.layout.fragment_road_status_item_map),GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener,GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener {
    var id:Int?=-1
    var gmap: GoogleMap? = null
    var currentRoadStatus: RoadStatus?=null
    var roadStatusData: JSONObject?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        road_status_item_mapView.onCreate(savedInstanceState)
        road_status_item_mapView.isClickable = true
        road_status_item_mapView.getMapAsync {
            gmap = it
            gmap?.setOnMapLoadedCallback(this)
            gmap?.setOnMapClickListener(this)
            gmap?.setOnMapLongClickListener(this)
            gmap?.setOnCameraIdleListener(this)
        }
        currentRoadStatus = DatabaseHandler().getItemById(view?.context,id!!)
        val appFolderPath = view?.context.getExternalFilesDir(null)?.absolutePath
        val folderName=currentRoadStatus!!.file_name

        val appFolder = File(appFolderPath, "PFA")
        val data = File(appFolder.absolutePath+"/"+folderName+"/data.json")



        roadStatusData = JSONObject(data.readLines().joinToString())

    }


    fun setDataToMap(){
        var lowSpeedPolyPath = PolylineOptions().color(Color.BLUE)
        lowSpeedPolyPath.startCap(RoundCap())
        lowSpeedPolyPath.endCap(RoundCap())



        var long = roadStatusData?.getJSONObject("0")?.get("Longitude") as Double
        var lat = roadStatusData?.getJSONObject("0")?.get("Latitude") as Double
        lowSpeedPolyPath.add(LatLng(lat,long))
        gmap?.addMarker(MarkerOptions().position(LatLng(lat,long)).title("Starting Location"))?.tag="starting location"
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat!!, long!!), 20.0f))

        for (i in 1 until roadStatusData!!.length()){
            val speed = roadStatusData?.getJSONObject(i.toString())?.get("speed") as Double
            val gyroX = roadStatusData?.getJSONObject(i.toString())?.get("Gyro-x")
            val info="Latitude: $lat, Longitute: $long"


            long = roadStatusData?.getJSONObject(i.toString())?.get("Longitude") as Double
            lat = roadStatusData?.getJSONObject(i.toString())?.get("Latitude") as Double

            lowSpeedPolyPath.add(LatLng(lat,long)).color(Color.BLUE)

            if(speed>5){
                gmap?.addMarker(MarkerOptions().position(LatLng(lat,long)).title("Position").snippet(info)
                        .icon(bitmapDescriptorFromVector(view?.context!!,R.drawable.marker_dot_icon, Color.RED)))?.tag=i
            }else{
                gmap?.addMarker(MarkerOptions().position(LatLng(lat,long)).title("Position").snippet(info)
                        .icon(bitmapDescriptorFromVector(view?.context!!,R.drawable.marker_dot_icon, Color.RED)))?.tag=i
            }



        }


        gmap?.addPolyline(lowSpeedPolyPath)

    }


    private fun showAllInfo(data:Map<String,String>){
        val builder = AlertDialog.Builder(view?.context!!)

        // Set the alert dialog title
        builder.setTitle("More information")

        var msg=""
        for((t,v) in data){
            msg += "$t : $v \n"
        }
        // Display a message on alert dialog
        builder.setMessage(msg)


        // Display a negative button on alert dialog
        builder.setNegativeButton("Hide"){ _, _ ->

        }


        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        road_status_item_mapView.onStart()

    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, color:Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            this.setTint(color)
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))

            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    override fun onResume() {
        super.onResume()
        road_status_item_mapView.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        road_status_item_mapView.onLowMemory()
    }



    override fun onMapClick(p0: LatLng?) {

    }

    override fun onMapLongClick(p0: LatLng?) {
    }

    override fun onCameraIdle() {
    }

    override fun onMapLoaded() {
        gmap?.setOnMarkerClickListener(this)
        setDataToMap()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        println("Marker tag:"+p0?.tag)
        if(!p0?.tag?.equals("starting location")!!)
        {
            val speed = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("speed") as Double
            val gyroX = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Gyro-x") as Double
            val gyroY = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Gyro-y") as Double
            val gyroZ = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Gyro-z") as Double
            val accX = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Acc-x") as Double
            val accY = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Acc-y") as Double
            val accZ = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Acc-z") as Double
            val lat = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Latitude") as Double
            val long = roadStatusData?.getJSONObject(p0?.tag.toString())?.get("Longitude") as Double
            val data = mapOf("speed" to "$speed KM/H",
                    "Gyroscope X" to gyroX.toString(),"Gyroscope Y" to gyroY.toString(),"Gyroscope Z" to gyroZ.toString(),
                    "Accelerometer X" to accX.toString(),"Accelerometer Y" to accY.toString(),"Accelerometer Z" to accZ.toString(),
                    "Latitude" to lat.toString(),"Longitude" to long.toString())

            showAllInfo(data)
        }
        return true
    }


}