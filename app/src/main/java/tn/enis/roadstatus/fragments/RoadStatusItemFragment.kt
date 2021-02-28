package tn.enis.roadstatus.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.android.synthetic.main.fragment_road_status_item.*
import org.json.JSONObject
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.io.File


class RoadStatusItemFragment : Fragment(R.layout.fragment_road_status_item),GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener,GoogleMap.OnMapLoadedCallback {

    var id:Int?=-1
    var gmap: GoogleMap? = null
    var currentRoadStatus:RoadStatus?=null
    var jsonObject:JSONObject?=null
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



        jsonObject = JSONObject(data.readLines().joinToString())



    }

    fun setDataToMap(){
        val long = jsonObject?.getJSONObject("0")?.get("Longitude") as Double
        val lat = jsonObject?.getJSONObject("0")?.get("Latitude") as Double
        println("LatLong($lat,$long)")
        gmap?.addMarker(MarkerOptions().position(LatLng(lat,long)).title("Asba"))
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat!!, long!!), 20.0f))

    }

    override fun onStart() {
        super.onStart()
        road_status_item_mapView.onStart()

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
        setDataToMap()
    }


}