package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Settings
import kotlin.math.round

abstract class MapFeatures : AppCompatActivity(), GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnPolylineClickListener {
    private var trajectoryPolyLine: Polyline? = null
    private val pattern = listOf(Dot(), Gap(20F), Dash(30F), Gap(20F))
    private var marker: Marker? = null
    private var startingPosition: LatLng? = null
    private var initialMarker: Marker? = null
    private var cp: Marker? = null
    private var url: String? = null
    private var pathPolyLine: PolylineOptions? = PolylineOptions()
    protected var longitude: Double? = 0.0
    protected var altitude: Double? = 0.0
    protected var latitude: Double? = 0.0
    protected var loc: Location? = null
    protected var pathPolylineOnMap: Polyline? = null
    protected var gmap: GoogleMap? = null
    protected var locationRequest: LocationRequest? = null
    protected var locationCallback: LocationCallback? = null
    protected var fusedLocationProviderClient: FusedLocationProviderClient? = null
    protected var polyline: PolylineOptions? = PolylineOptions()
    protected val satelliteStyleButton: RadioButton by lazy {
        findViewById(R.id.satelliteStyle)
    }
    protected val mapStyleButton: RadioButton by lazy {
        findViewById(R.id.mapStyle)
    }

    protected var timerStarted: Long? = 0L
    protected var speedText: TextView? = null
    protected var chrono: Chronometer? = null

    protected var speed: Float = 0f
    private lateinit var pathPoints: JSONArray
    protected val settings = Settings()


    abstract fun updateLocation()

    fun initializeGoogleMap(googleMap:GoogleMap)
    {
        gmap = googleMap
        gmap?.setOnMapClickListener(this)
        gmap?.setOnMapLongClickListener(this)
        gmap?.setOnCameraIdleListener(this)
        gmap?.setOnMapLoadedCallback(this)
        gmap?.setOnPolylineClickListener(this)
        gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
    }
    //function to update the auto generated path when user is close to one of its points by removing it and redrawing the polyline
    fun checkNavigationPath() {

        if (pathPolyLine != null) {
            if (pathPolyLine?.points?.size!! > 0) {
                val pathPolylineNextPointLocation = Location("")
                val newLine = PolylineOptions()
                for (i in 0 until pathPolyLine?.points?.size!!) {
                    newLine.add(
                        LatLng(
                            pathPolyLine?.points?.get(i)?.latitude!!,
                            pathPolyLine?.points?.get(i)?.longitude!!
                        )
                    )
                }

                for (i in 0 until pathPolyLine?.points?.size!!) {
                    if (newLine.points.size > 0) {
                        if (newLine.points.indexOf(pathPolyLine?.points?.get(i)) != -1) {
                            val pos =
                                newLine.points[newLine.points.indexOf(pathPolyLine?.points?.get(i))]

                            pathPolylineNextPointLocation.latitude = pos.latitude
                            pathPolylineNextPointLocation.longitude = pos.longitude

                            if (loc?.distanceTo(pathPolylineNextPointLocation)!! > Constants.MIN_DISTANCE_TO_REMOVE_PT) {
                                newLine.points.removeAt(
                                    newLine.points.indexOf(
                                        pathPolyLine?.points?.get(
                                            i
                                        )
                                    )
                                )
                            }
                        }
                    }

                }
                pathPolyLine = newLine
                drawPathPolyline()
            }
        }
    }

    //draws a polyline which leads to a point marker on the map with a long click
    private fun drawPathPolyline() {
        pathPolylineOnMap?.remove()
        pathPolyLine?.color(Color.BLUE)
        pathPolylineOnMap = gmap?.addPolyline(pathPolyLine)
        pathPolylineOnMap?.tag = "path"
        pathPolylineOnMap?.isClickable = true
        pathPolylineOnMap?.pattern = pattern
        pathPolylineOnMap?.width = 20f
    }

    //updates the map with the path that the user took by drawing a polyline
    fun setPolyLineOnMap() {
        trajectoryPolyLine?.remove()
        trajectoryPolyLine = gmap?.addPolyline(polyline)

    }

    //function that tells the device to make location updates
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun removePredictedPathPolyline(polyline: Polyline?) {
        if (polyline?.tag == "path") {
            pathPolylineOnMap?.remove()
            marker?.remove()
            pathPolylineOnMap = null
        }
    }

    //function to get the devices' current location only once
    @SuppressLint("MissingPermission")
    fun getDeviceLocation() {
        fusedLocationProviderClient?.flushLocations()
        startingPosition = null
        val locationResult = fusedLocationProviderClient?.lastLocation
        locationResult?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                if (task.result != null) {
                    loc = task.result

                    longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                    altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                    latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                    startingPosition = LatLng(latitude!!, longitude!!)

                    timerStarted = System.currentTimeMillis()
                    chrono?.base = SystemClock.elapsedRealtime()
                    chrono?.start()
                    updateUI()
                    speed = 0f
                    setInitialPositionMarker()
                    polyline?.add(startingPosition)

                    // Set the map's camera position to the current location of the device.
                    gmap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                loc!!.latitude,
                                loc!!.longitude
                            ), 20f
                        )
                    )

                }
            }
        }
    }
    //updates the speed with the current speed and updates the textview made for it
    @SuppressLint("SetTextI18n")
    fun updateUI() {
        GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                speed = if (loc!!.hasSpeed()) round(loc!!.speed * 3.6).toFloat() else 0f
                speedText?.text = "$speed KM/H"
                delay(500)
            }
        }
    }

    private fun setInitialPositionMarker() {
        if (initialMarker == null) {
            initialMarker = gmap?.addMarker(
                startingPosition?.let {
                    MarkerOptions().position(it)
                        .title("Initial Position")
                }
            )
        }
    }

    fun setCurrentPositionMarker() {
        cp?.remove()
        cp = gmap?.addMarker(
            MarkerOptions().position(LatLng(latitude!!, longitude!!)).title("Current Position")
        )
    }

    private fun requestDestinationPolyline(position: LatLng?)
    {
        url = "https://api.tomtom.com/routing/1/calculateRoute/"
        marker?.remove()
        marker = gmap?.addMarker(
            MarkerOptions().position(position!!)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        url += "${loc?.latitude},${loc?.longitude}:${position?.latitude},${position?.longitude}/json?key=Vstg8Js5WPgqQJdWwXEyJF3XPzElvdCi"

        request(url!!)
        checkNavigationPath()
    }
    //makes a http request and gets a response with a json string containing path points
    private fun request(url: String) {
        val queue = Volley.newRequestQueue(this)
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                val jsonObject = JSONObject(response)
                val jsonArray: JSONArray = jsonObject.getJSONArray("routes")
                pathPolyLine = PolylineOptions()
                pathPoints = jsonArray.getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONArray("points")
                for (i in 0 until pathPoints.length()) {
                    val lat = pathPoints.getJSONObject(i).get("latitude") as Double
                    val lon = pathPoints.getJSONObject(i).get("longitude") as Double

                    pathPolyLine?.add(LatLng(lat, lon))
                }
                drawPathPolyline()
            },
            {

            })

        // Add the request to the RequestQueue.

        queue.add(stringRequest)

    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

    }


    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onMapClick(p0: LatLng?) {
    }

    override fun onMapLongClick(position: LatLng?) {
        requestDestinationPolyline(position)
    }

    override fun onCameraIdle() {

    }


    override fun onDestroy() {
        gmap?.clear()
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onPolylineClick(p0: Polyline?) {
        removePredictedPathPolyline(p0)
    }

}