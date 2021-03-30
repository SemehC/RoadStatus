package tn.enis.roadstatus.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment

import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast.*
import androidx.annotation.MenuRes

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*

import kotlinx.android.synthetic.main.fragment_road_status_item_map.*
import org.json.JSONObject
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.io.File

class RoadStatusItemMapFragment : Fragment(R.layout.fragment_road_status_item_map),
    GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener,
    GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener {
    var id: Int? = -1
    var gmap: GoogleMap? = null
    var currentRoadStatus: RoadStatus? = null
    var roadStatusData: JSONObject? = null
    var mapView: MapView? = null
    var lastMarker: Marker? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //find the google map that's in the layout
        mapView = view.findViewById(R.id.road_status_item_mapView)
        //initialize the map
        mapView?.onCreate(savedInstanceState)

        //set it clickable
        mapView?.isClickable = true
        mapView?.getMapAsync {
            //register events when interacting with the map and process them in this class
            gmap = it
            gmap?.setOnMapLoadedCallback(this)
            gmap?.setOnMapClickListener(this)
            gmap?.setOnMapLongClickListener(this)
            gmap?.setOnCameraIdleListener(this)
            gmap?.setOnPolylineClickListener(this)
            gmap?.setOnMarkerClickListener(this)
            gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        }

        val satelliteStyleButton: RadioButton = view.findViewById(R.id.satelliteStyle)

        val mapStyleButton: RadioButton = view.findViewById(R.id.mapStyle)

        satelliteStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        mapStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        }

        //Get the data out of the database for a particular route clicked in the home activity
        currentRoadStatus = DatabaseHandler().getItemById(view.context, id!!)

        val appFolderPath = view.context.getExternalFilesDir(null)?.absolutePath
        val folderName = currentRoadStatus!!.file_name
        val appFolder = File(appFolderPath, "PFA")
        val data = File(appFolder.absolutePath + "/" + folderName + "/data.json")

        //find the button that shows a menu of points that were registered during the sampling process
        val menuButton = view.findViewById<Button>(R.id.menu_button)
        //make it show a menu of those points when clicked by calling the function showMenu
        menuButton.setOnClickListener {
            showMenu(it, R.menu.popup_menu)
        }

        //load the data from the json file
        roadStatusData = JSONObject(data.readLines().joinToString())

    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        //load the desired popup menu view from resources/menu
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)
        for (i in 1 until roadStatusData!!.length()) {
            //dynamically add points to the menu and add corresponding data to each point
            popup.menu.add("$i").setOnMenuItemClickListener {
                val long = roadStatusData?.getJSONObject(i.toString())?.get("Longitude") as Double
                val lat = roadStatusData?.getJSONObject(i.toString())?.get("Latitude") as Double
                val info = "Latitude: $lat, Longitute: $long"

                //add a marker on the desired point and remove the last one if it exists
                lastMarker?.remove()
                lastMarker = gmap?.addMarker(
                    MarkerOptions().position(LatLng(lat, long)).title("Position").snippet(info)
                        .icon(
                            bitmapDescriptorFromVector(
                                view?.context!!,
                                R.drawable.marker_dot_icon,
                                Color.RED
                            )
                        ).visible(true)
                )
                lastMarker!!.tag = i
                //move the camera to the desired point when clicked
                gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), 20.0f))
                return@setOnMenuItemClickListener true

            }
        }


        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun setDataToMap() {

        //list of polyline to be drawn when ready
        val polyLines = ArrayList<PolylineOptions>()

        //points that make the polyline, with which we determine the length of the polyline ( either red or blue one)
        val points = ArrayList<LatLng>()

        //locate the starting position
        val long = roadStatusData?.getJSONObject("0")?.get("Longitude") as Double
        val lat = roadStatusData?.getJSONObject("0")?.get("Latitude") as Double

        //mark the starting location on the map
        gmap?.addMarker(
            MarkerOptions().position(LatLng(lat, long)).title("Starting location")
        )?.tag = "starting location"
        //move the camera to that point
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), 20.0f))

        var gotHighSpeed = false
        //loop through all points of the road , if point's speed > 4 then we add it to the red polyline else to the blue polyline then draw those polylines
        for (i in 0 until roadStatusData!!.length()) {
            val longitude = roadStatusData?.getJSONObject(i.toString())?.get("Longitude") as Double
            val latitude = roadStatusData?.getJSONObject(i.toString())?.get("Latitude") as Double

            var oldlat: Double? = null
            var oldlong: Double? = null
            if (i > 0) {
                oldlong =
                    roadStatusData?.getJSONObject((i - 1).toString())?.get("Longitude") as Double
                oldlat =
                    roadStatusData?.getJSONObject((i - 1).toString())?.get("Latitude") as Double
            }


            val sp = roadStatusData?.getJSONObject(i.toString())?.get("speed") as Double

            if (sp < 4) {
                if (gotHighSpeed && points.size != 0) {
                    points.add(LatLng(oldlat!!, oldlong!!))
                    points.add(LatLng(latitude, longitude))
                    polyLines.add(generatePolyLine(points, Color.RED))
                    points.clear()
                    gotHighSpeed = false
                }
                points.add(LatLng(latitude, longitude))
            }

            if (sp >= 4) {
                if (!gotHighSpeed && points.size != 0) {

                    points.add(LatLng(oldlat!!, oldlong!!))
                    points.add(LatLng(latitude, longitude))
                    polyLines.add(generatePolyLine(points, Color.BLUE))
                    points.clear()
                    gotHighSpeed = true
                }
                points.add(LatLng(latitude, longitude))
            }


            if (points.size != 0) {
                if (gotHighSpeed) {
                    polyLines.add(generatePolyLine(points, Color.RED))
                } else {
                    polyLines.add(generatePolyLine(points, Color.BLUE))
                }
            }

            //mark last point as the ending location
            if (i == roadStatusData!!.length() - 1) {
                gmap?.addMarker(
                    MarkerOptions().position(LatLng(latitude, longitude)).title("Ending location")
                )?.tag = "ending location"
            }
        }
        //draw each polyline in the array on the map
        polyLines.forEach {
            println("Color : ${it.color} || size ${it.points.size}")
            gmap?.addPolyline(it)
        }


    }
    //function to create a polyline with given points and given color
    private fun generatePolyLine(pts: ArrayList<LatLng>, color: Int): PolylineOptions {
        val poly = PolylineOptions().color(color)
        poly.width(10f)
        poly.startCap(RoundCap())
        poly.endCap(RoundCap())

        pts.forEach {
            poly.add(it)
        }

        return poly

    }

    //function to make a dialog showing all info about a particular point
    private fun showAllInfo(data: Map<String, String>) {
        val builder = AlertDialog.Builder(view?.context!!)

        // Set the alert dialog title
        builder.setTitle("More information")

        var msg = ""
        for ((t, v) in data) {
            msg += "$t : $v \n"
        }
        // Display a message on alert dialog
        builder.setMessage(msg)


        // Display a negative button on alert dialog
        builder.setNegativeButton("Hide") { _, _ ->

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

    //function to make markers in round shape
    private fun bitmapDescriptorFromVector(
        context: Context,
        vectorResId: Int,
        color: Int
    ): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            this.setTint(color)
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
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


    override fun onMapClick(position: LatLng?) {
    }

    override fun onMapLongClick(p0: LatLng?) {
    }

    override fun onCameraIdle() {
    }


    override fun onMapLoaded() {
        //when the map is loaded , show all data for that road
        setDataToMap()
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        //when a marker is clicked , get all the data for the marker's position from the json file and pass it to the showAllInfo function
        println("Marker tag:" + marker?.tag)
        if (!marker?.tag?.equals("starting location")!! && !marker.tag?.equals("ending location")!!) {
            if (lastMarker != null)
                lastMarker!!.isVisible = false
            lastMarker = marker
            marker.isVisible = true
            val speed = roadStatusData?.getJSONObject(marker.tag.toString())?.get("speed") as Double
            val gyroX =
                roadStatusData?.getJSONObject(marker.tag.toString())?.get("Gyro-x") as Double
            val gyroY =
                roadStatusData?.getJSONObject(marker.tag.toString())?.get("Gyro-y") as Double
            val gyroZ =
                roadStatusData?.getJSONObject(marker.tag.toString())?.get("Gyro-z") as Double
            val accX = roadStatusData?.getJSONObject(marker.tag.toString())?.get("Acc-x") as Double
            val accY = roadStatusData?.getJSONObject(marker.tag.toString())?.get("Acc-y") as Double
            val accZ = roadStatusData?.getJSONObject(marker.tag.toString())?.get("Acc-z") as Double
            val lat =
                roadStatusData?.getJSONObject(marker.tag.toString())?.get("Latitude") as Double
            val long =
                roadStatusData?.getJSONObject(marker.tag.toString())?.get("Longitude") as Double
            val data = mapOf(
                "speed" to "$speed KM/H",
                "Gyroscope X" to gyroX.toString(),
                "Gyroscope Y" to gyroY.toString(),
                "Gyroscope Z" to gyroZ.toString(),
                "Accelerometer X" to accX.toString(),
                "Accelerometer Y" to accY.toString(),
                "Accelerometer Z" to accZ.toString(),
                "Latitude" to lat.toString(),
                "Longitude" to long.toString()
            )

            showAllInfo(data)
        } else {
            marker.showInfoWindow()
        }
        return true
    }

    override fun onDestroyView() {
        gmap?.clear()
        mapView?.onDestroy()
        mapView?.removeAllViews()
        super.onDestroyView()
    }

    override fun onPolylineClick(polyline: Polyline?) {

    }


}