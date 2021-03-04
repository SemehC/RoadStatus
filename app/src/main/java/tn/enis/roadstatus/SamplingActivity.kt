package tn.enis.roadstatus

import  android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color

import android.hardware.Sensor
import android.hardware.SensorManager

import android.location.Location
import android.location.LocationManager

import android.os.Bundle

import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import tn.enis.roadstatus.listeners.AccelerometerListener
import tn.enis.roadstatus.listeners.GyroscopeListener
import tn.enis.roadstatus.other.Constants.GPS_ACCURACY
import tn.enis.roadstatus.other.Constants.MAX_DISTANCE_BETWEEN_POINTS
import tn.enis.roadstatus.other.Utilities

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round


class SamplingActivity : AppCompatActivity(), GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback {

    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var marker: Marker? = null
    private val dbManager by lazy {
        DatabaseHandler()
    }


    private var url: String? = null
    private var isRecording = false
    private var accSensor: Sensor? = null
    private var gyro: Sensor? = null
    private var sensorManager: SensorManager? = null
    private var loc: Location? = null
    private var speed: Float = 0f
    private var index: Int = 0
    private var endFile: String = ""
    private var map = mutableMapOf<Int, Map<String, String>>()

    private var gmap: GoogleMap? = null
    private var polyline: PolylineOptions? = PolylineOptions()
    private var pathPolyLine: PolylineOptions? = PolylineOptions()
    private var pathPolylineOnMap: Polyline? = null


    private var longitude: Double? = 0.0
    private var altitude: Double? = 0.0
    private var latitude: Double? = 0.0

    private var timerStarted: Long? = 0L
    private var timer: Long = 0L
    private var speedText: TextView? = null
    private var timeText: TextView? = null

    private var samlplingDelay: Long = 1000L

    private var gManager: GyroscopeListener = GyroscopeListener()
    private var accManager: AccelerometerListener = AccelerometerListener()

    private var locationManager: LocationManager? = null
    private var locationObtained: Boolean = false

    private var stillScanning: Boolean = true

    private var appFolderPath: String? = null
    private lateinit var folderName: String
    private var appFolder: File? = null
    private var filesFolder: File? = null
    private var cameraIsOpened = false

    private val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
    }
    private val satelliteStyleButton: Button by lazy {
        findViewById(R.id.satelliteStyle)
    }
    private val mapStyleButton: Button by lazy {
        findViewById(R.id.mapStyle)
    }
    private val startStopRecording: ImageButton by lazy {
        findViewById(R.id.recordVideoButton)
    }
    private val openCameraButton: ImageButton by lazy {
        findViewById(R.id.cameraButton)
    }

    private lateinit var pathPoints: JSONArray
    private var startingPosition: LatLng? = null
    private var im: Marker? = null
    private var cp: Marker? = null
    private var trajectoryPolyLine: Polyline? = null
    private lateinit var gyroData: Array<Double>
    private lateinit var accData: Array<Double>
    private var deviceCameraManager: DeviceCameraManager? = null


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        createFolders()

        //Initializing Camera Manager
        deviceCameraManager = DeviceCameraManager(filesFolder!!, this, videoPreview)

        //Initializing Location Manager
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
        //Getting Sensors Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        //Stop Button Clicked !!
        stopButton.setOnClickListener {
            saveToDatabase(folderName)
            GlobalScope.launch(Dispatchers.Default) {
                if (isRecording) {
                    deviceCameraManager!!.stopRecording()
                    isRecording = false
                }
                saveFile()
            }
            stillScanning = false
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //Record or stop the recording
        startStopRecording.setOnClickListener {
            if (isRecording) {
                deviceCameraManager!!.stopRecording()
                deviceCameraManager!!.previewSession()
                startStopRecording.setImageResource(android.R.drawable.presence_online)
                Toast.makeText(this, "Stopped recording !", Toast.LENGTH_SHORT).show()
            } else {
                deviceCameraManager!!.recordSession()
                startStopRecording.setImageResource(android.R.drawable.ic_notification_overlay)
                Toast.makeText(this, "Started recording !", Toast.LENGTH_SHORT).show()
            }
            isRecording = !isRecording
        }

        //Open or close the camera
        openCameraButton.setOnClickListener {
            if (!isRecording) {
                if (cameraIsOpened) {
                    deviceCameraManager!!.closeCamera()
                    videoPreview.isVisible = false
                    startStopRecording.isEnabled = false
                    startStopRecording.isClickable = false
                    startStopRecording.isVisible = false

                    openCameraButton.setImageResource(android.R.drawable.presence_video_online)
                    Toast.makeText(this, "Camera closed !", Toast.LENGTH_SHORT).show()
                } else {
                    videoPreview.isVisible = true
                    deviceCameraManager!!.connectCamera()
                    startStopRecording.isEnabled = true
                    startStopRecording.isClickable = true
                    startStopRecording.isVisible = true

                    openCameraButton.setImageResource(android.R.drawable.presence_video_busy)
                    Toast.makeText(this, "Camera opened !", Toast.LENGTH_SHORT).show()
                }
                cameraIsOpened = !cameraIsOpened
            } else {
                Toast.makeText(this, "Please stop recording first !", Toast.LENGTH_SHORT).show()
            }

        }
        initUI()
    }


    private fun updateLocation() {
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.smallestDisplacement= MAX_DISTANCE_BETWEEN_POINTS
        locationRequest?.interval = 1000
        locationRequest?.fastestInterval = 500
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                loc = locationResult.lastLocation

                    if(loc?.hasAccuracy() == true){
                        if(loc?.accuracy!!< GPS_ACCURACY){
                            setCurrentPositionMarker()
                            longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                            altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                            latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                            speed = if (loc!!.hasSpeed()) (loc!!.speed * 3.6).toFloat() else 0f
                            polyline?.add(LatLng(latitude!!, longitude!!))
                            setCurrentPositionMarker()
                            setPolyLineOnMap()
                            if(pathPolylineOnMap?.points?.size!! >0)
                            {

                                var pathPolylineNextPointLocation = Location("")
                                pathPolylineNextPointLocation.latitude=pathPolylineOnMap?.points?.get(0)!!.latitude
                                pathPolylineNextPointLocation.longitude=pathPolylineOnMap?.points?.get(0)!!.longitude
                                if(loc?.distanceTo(pathPolylineNextPointLocation)!! < 3f)
                                {
                                    pathPolylineOnMap!!.points.removeAt(0)
                                }
                            }
                        }
                    }






            }
        }
    }


    private fun initUI() {

        startStopRecording.isVisible = false
        startStopRecording.isEnabled = false
        startStopRecording.isClickable = false
    }


    private fun createFolders() {
        appFolderPath = this.getExternalFilesDir(null)?.absolutePath
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        folderName = simpleDateFormat.format(Date())
        appFolder = File(appFolderPath, "PFA")
        filesFolder = File(appFolder!!.absolutePath, folderName)
        if (!appFolder?.exists()!!) {
            appFolder?.mkdir()
        }
        if (!filesFolder!!.exists()) {
            filesFolder!!.mkdir()
        }

    }


    private suspend fun saveFile() {
        val file = File(filesFolder!!.absolutePath + "/data.json")
        withContext(Dispatchers.IO) {
            while (!file.exists()) {

                try {
                    val gson = Gson()
                    endFile += gson.toJson(map)
                    val fw = FileWriter(file.absoluteFile)
                    val bw = BufferedWriter(fw)
                    bw.write(endFile)
                    bw.flush()
                    bw.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    System.exit(-1)
                }
            }

        }
    }

    private fun startScanning() {
        GlobalScope.launch {
            scanning()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        sensorManager!!.registerListener(accManager, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(gManager, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        deviceCameraManager!!.startBackgroundThread()
        if (videoPreview.isAvailable)
        else
            videoPreview.surfaceTextureListener = deviceCameraManager!!.surfaceListener
        startLocationUpdates()
    }

    override fun onPause() {

        super.onPause()
        deviceCameraManager!!.closeCamera()
        deviceCameraManager!!.stopBackgroundThread()
        mapView.onPause()
        sensorManager!!.unregisterListener(accManager)
        sensorManager!!.unregisterListener(gManager)
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

    @SuppressLint("MissingPermission")
    private suspend fun scanning() {
        while (stillScanning) {
            timer = System.currentTimeMillis() - timerStarted!!
            gyroData = gManager.getData()
            accData = accManager.getData()
            gotData()
            delay(samlplingDelay)
        }
    }

    private fun saveToDatabase(fName: String) {
        gmap?.snapshot {
            dbManager.saveRoadStatus(RoadStatus("Scan", it, timerStarted!!, timer, getTravelDistance(), fName), this)

        }

    }

    private fun getTravelDistance(): Float {
        return Utilities.calculateTotalDistance(polyline!!)
    }

    private fun gotData() {
        GlobalScope.launch {
            checkSpeed()
            addData()
            updateUI()
        }
    }

    private suspend fun checkSpeed() {
        withContext(Dispatchers.Default) {
            if (loc?.hasSpeed() == true) {
                speed = loc?.speed!!
            } else {
                speed = 0f
            }
        }
    }

    private suspend fun updateUI() {
        withContext(Dispatchers.Main) {
            speedText?.text = (round(speed) * 3.6).toString() + " KM/H"
            timeText?.text = TimeUnit.MILLISECONDS.toSeconds(timer).toString()
        }
    }

    private suspend fun addData() {
        withContext(Dispatchers.Default) {
            var array = mapOf("speed" to round(speed * 3.6), "Gyro-x" to gyroData[0], "Gyro-y" to gyroData[1], "Gyro-z" to gyroData[2], "Acc-x" to accData[0], "Acc-y" to accData[1], "Acc-z" to accData[2], "Longitude" to longitude, "Latitude" to latitude, "Altitude" to altitude)
            map[index] = array as Map<String, String>
            index++
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

    private fun startTimer() {
        timerStarted = System.currentTimeMillis()
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

                    longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                    altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                    latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                    startingPosition = LatLng(latitude!!, longitude!!)
                    startScanning()
                    startTimer()
                    updateMapUI()
                    polyline?.add(startingPosition)
                    gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
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
        marker = gmap?.addMarker(MarkerOptions().position(position!!).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        url += "${loc?.latitude},${loc?.longitude}:${position?.latitude},${position?.longitude}/json?key=Vstg8Js5WPgqQJdWwXEyJF3XPzElvdCi"
        // var response = URL(url).readText()


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
        val stringRequest = StringRequest(Request.Method.GET, url,
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
                    updateMapUI()

                },
                {

                })

        // Add the request to the RequestQueue.

        queue.add(stringRequest)

    }

    override fun onCameraIdle() {

    }


    override fun onBackPressed() {
        Toast.makeText(this, "Back button disabled", Toast.LENGTH_SHORT).show()
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

}


