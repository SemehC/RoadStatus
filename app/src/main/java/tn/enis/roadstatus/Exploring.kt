package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import tn.enis.roadstatus.listeners.AccelerometerListener
import tn.enis.roadstatus.listeners.GyroscopeListener
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Utilities
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.round


class Exploring : AppCompatActivity(), GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnPolylineClickListener {
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var marker: Marker? = null
    private var url: String? = null
    private var loc: Location? = null
    private var speed: Float = 0f
    private var gmap: GoogleMap? = null
    private var polyline: PolylineOptions? = PolylineOptions()
    private var pathPolyLine: PolylineOptions? = PolylineOptions()
    private var pathPolylineOnMap: Polyline? = null


    private var longitude: Double? = 0.0
    private var altitude: Double? = 0.0
    private var latitude: Double? = 0.0

    private var speedText: TextView? = null
    private var timeText: TextView? = null
    private var locationManager: LocationManager? = null
    private var locationObtained: Boolean = false
    private val pattern = listOf(Dot(), Gap(20F), Dash(30F), Gap(20F))
    private var appFolderPath: String? = null
    private lateinit var folderName: String
    private var appFolder: File? = null
    private var filesFolder: File? = null

    private val satelliteStyleButton: Button by lazy {
        findViewById(R.id.satelliteStyle)
    }
    private val mapStyleButton: Button by lazy {
        findViewById(R.id.mapStyle)
    }
    private lateinit var pathPoints: JSONArray
    private var startingPosition: LatLng? = null
    private var im: Marker? = null
    private var cp: Marker? = null
    private var trajectoryPolyLine: Polyline? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploring)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)
        timeText = findViewById(R.id.time_text_view)

        //Getting Google Map
        mapView.onCreate(savedInstanceState)
        mapView.isClickable = true

        mapView.getMapAsync {
            gmap = it
            gmap?.setOnMapClickListener(this)
            gmap?.setOnMapLongClickListener(this)
            gmap?.setOnCameraIdleListener(this)
            gmap?.setOnMapLoadedCallback(this)
            gmap?.setOnPolylineClickListener(this)
            gmap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        satelliteStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        mapStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        }

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        updateLocation()


    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdates()
    }

    override fun onPause() {

        super.onPause()

        mapView.onPause()

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

    private fun updateLocation() {
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.smallestDisplacement = Constants.MAX_DISTANCE_BETWEEN_POINTS
        locationRequest?.interval = 1000
        locationRequest?.fastestInterval = 500
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                loc = locationResult.lastLocation

                if (loc?.hasAccuracy() == true) {
                    if (loc?.accuracy!! < Constants.GPS_ACCURACY) {
                        setCurrentPositionMarker()
                        longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                        altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                        latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                        speed = if (loc!!.hasSpeed()) (loc!!.speed * 3.6).toFloat() else 0f
                        polyline?.add(LatLng(latitude!!, longitude!!))
                        setCurrentPositionMarker()
                        setPolyLineOnMap()
                        if (pathPolylineOnMap != null) {
                            if (pathPolylineOnMap?.points?.size!! > 0) {
                                var pathPolylineNextPointLocation = Location("")
                                pathPolylineNextPointLocation.latitude =
                                    pathPolylineOnMap?.points?.get(0)!!.latitude
                                pathPolylineNextPointLocation.longitude =
                                    pathPolylineOnMap?.points?.get(0)!!.longitude
                                if (loc?.distanceTo(pathPolylineNextPointLocation)!! < 3f) {
                                    pathPolylineOnMap!!.points.removeAt(0)
                                }
                            }
                        }

                    }
                }
            }
        }

    }
    private fun startSpeedChecking() {
        GlobalScope.launch {
            checkSpeed()
        }
    }
    private suspend fun checkSpeed() {
        withContext(Dispatchers.Default) {
            if (loc?.hasSpeed() == true) {
                speed = loc?.speed!!
            } else {
                speed = 0f
            }
            updateUI()
        }
    }

    private suspend fun updateUI() {
        withContext(Dispatchers.Main) {
            speedText?.text = (round(speed) * 3.6).toString() + " KM/H"
        }
    }


    private fun setCurrentPositionMarker() {
        cp?.remove()
        cp = gmap?.addMarker(
            MarkerOptions().position(LatLng(latitude!!, longitude!!)).title("Current Position")
        )
    }

    private fun updateMapUI() {
        if (im == null) {
            im = gmap?.addMarker(
                startingPosition?.let {
                    MarkerOptions().position(it)
                        .title("Initial Position")
                }
            )
        }
        gmap?.addPolyline(polyline)
    }


    private fun setPolyLineOnMap() {
        trajectoryPolyLine?.remove()
        trajectoryPolyLine = gmap?.addPolyline(polyline)

    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        startingPosition = null
        val locationResult = fusedLocationProviderClient?.lastLocation
        locationResult?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

                // Set the map's camera position to the current location of the device.
                lastKnownLocation = task.result
                if (lastKnownLocation != null) {
                    locationObtained = true
                    loc = lastKnownLocation
                    startSpeedChecking()

                    longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                    altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                    latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                    startingPosition = LatLng(latitude!!, longitude!!)
                    updateMapUI()
                    polyline?.add(startingPosition)
                    gmap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                        LatLng(loc!!.latitude,
                            loc!!.longitude), 20f))
                }
            }
        }
    }
    //define the listener

    override fun onMapClick(p0: LatLng?) {
    }

    override fun onMapLongClick(position: LatLng?) {
        url = "https://api.tomtom.com/routing/1/calculateRoute/"
        marker?.remove()
        marker = gmap?.addMarker(
            MarkerOptions().position(position!!).icon(
                BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_AZURE)))
        url += "${loc?.latitude},${loc?.longitude}:${position?.latitude},${position?.longitude}/json?key=Vstg8Js5WPgqQJdWwXEyJF3XPzElvdCi"


        request(url!!)
        updateMapUI()


    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest,
            locationCallback,
            null)
    }




    fun request(url: String) {
        val queue = Volley.newRequestQueue(this)
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                var jsonObject = JSONObject(response)
                val jsonArray: JSONArray = jsonObject.getJSONArray("routes")
                pathPolyLine = PolylineOptions().color(Color.BLUE)
                pathPoints = jsonArray.getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONArray("points")
                for (i in 0 until pathPoints.length()) {
                    var lat = pathPoints.getJSONObject(i).get("latitude") as Double
                    var lon = pathPoints.getJSONObject(i).get("longitude") as Double

                    pathPolyLine?.add(LatLng(lat, lon))
                }
                pathPolylineOnMap?.remove()
                pathPolylineOnMap = gmap?.addPolyline(pathPolyLine)
                pathPolylineOnMap?.tag="path"
                pathPolylineOnMap?.isClickable=true
                pathPolylineOnMap?.pattern=pattern
                pathPolylineOnMap?.width = 20f
                updateMapUI()

            },
            {

            })

        // Add the request to the RequestQueue.

        queue.add(stringRequest)

    }

    override fun onCameraIdle() {

    }
    

    override fun onMapLoaded() {
        getDeviceLocation()
        startLocationUpdates()
    }

    override fun onDestroy() {
        gmap?.clear()
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onPolylineClick(p0: Polyline?) {
        if(p0?.tag=="path"){
            pathPolylineOnMap?.remove()
            marker?.remove()
            pathPolylineOnMap=null
        }
    }
}