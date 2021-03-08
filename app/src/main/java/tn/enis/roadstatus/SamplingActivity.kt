package tn.enis.roadstatus

import   android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color

import android.hardware.Sensor
import android.hardware.SensorManager

import android.location.Location
import android.location.LocationManager

import android.os.Bundle
import android.os.SystemClock
import android.widget.*

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
import tn.enis.roadstatus.other.Constants.MIN_DISTANCE_TO_REMOVE_PT
import tn.enis.roadstatus.other.Settings
import tn.enis.roadstatus.other.Utilities

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round


class SamplingActivity() : AppCompatActivity(), GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnPolylineClickListener {

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
    private var endFile: String = "id,speed,accx,accy,accz,gyrx,gyry,gyrz,lat,lng\n"
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
    private var chrono: Chronometer? = null


    private var gManager: GyroscopeListener = GyroscopeListener()
    private var accManager: AccelerometerListener = AccelerometerListener()

    private var locationManager: LocationManager? = null

    private var stillScanning: Boolean = true

    private var appFolderPath: String? = null
    private lateinit var folderName: String
    private var appFolder: File? = null
    private var filesFolder: File? = null
    private var cameraIsOpened = false

    private val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
    }
    private val satelliteStyleButton: RadioButton by lazy {
        findViewById(R.id.satelliteStyle)
    }
    private val mapStyleButton: RadioButton by lazy {
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
    private var initialMarker: Marker? = null
    private var cp: Marker? = null
    private var trajectoryPolyLine: Polyline? = null
    private lateinit var gyroData: Array<Double>
    private lateinit var accData: Array<Double>
    private var deviceCameraManager: DeviceCameraManager? = null
    private val pattern = listOf(Dot(), Gap(20F), Dash(30F), Gap(20F))


    private val settings = Settings()
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        settings.context = this

        settings.loadSettings()
        createFolders()
        //Initializing Camera Manager
        deviceCameraManager = DeviceCameraManager(filesFolder!!, this, videoPreview)
        //Initializing Location Manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)
        chrono = findViewById(R.id.time_text_view)

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
            gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
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

    //request location updates and update the ui with new location aswell as register accelerometer and gyroscope data
    private fun updateLocation() {
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.smallestDisplacement = settings.distanceBetweenPoints.toFloat()
        println("distance = ${settings.distanceBetweenPoints}")
        locationRequest?.interval = 1000
        locationRequest?.fastestInterval = 500
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                loc = locationResult.lastLocation
                if (loc?.hasAccuracy() == true) {
                    if (loc?.accuracy!! < GPS_ACCURACY) {


                        setCurrentPositionMarker()
                        longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                        altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                        latitude = if (loc?.latitude == null) 0.0 else loc?.latitude

                        gyroData = gManager.getData()
                        accData = accManager.getData()
                        gotData()


                        polyline?.add(LatLng(latitude!!, longitude!!))

                        //move camera to current position
                        gmap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    latitude!!,
                                    longitude!!
                                ), 20f
                            )
                        )
                        setCurrentPositionMarker()
                        setPolyLineOnMap()


                    }
                }
            }
        }
    }

    //function to update the auto generated path when user is close to one of its points by removing it and redrawing the polyline
    private fun checkNavigationPath() {

        if (pathPolyLine != null) {
            if (pathPolyLine?.points?.size!! > 0) {
                var pathPolylineNextPointLocation = Location("")
                var newLine = PolylineOptions()
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
                        if (newLine?.points.indexOf(pathPolyLine?.points?.get(i)) != -1) {
                            val pos =
                                newLine?.points[newLine?.points.indexOf(pathPolyLine?.points?.get(i))]

                            pathPolylineNextPointLocation.latitude = pos.latitude
                            pathPolylineNextPointLocation.longitude = pos.longitude

                            if (loc?.distanceTo(pathPolylineNextPointLocation)!! > MIN_DISTANCE_TO_REMOVE_PT) {
                                newLine?.points.removeAt(
                                    newLine?.points.indexOf(
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

    //make the recording button invisible on startup
    private fun initUI() {

        startStopRecording.isVisible = false
        startStopRecording.isEnabled = false
        startStopRecording.isClickable = false
    }

    //creates folders inside the app's folder , with its name being the current date
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

    //writes the data acquired by the sensors to a json file
    private suspend fun saveFile() {
        val file = File(filesFolder!!.absolutePath + "/data.csv")
        withContext(Dispatchers.IO) {
            while (!file.exists()) {

                try {
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

    //saves the time elapsed, folder name , travel distance ,time when started and a snapshot of the map in the database
    private fun saveToDatabase(fName: String) {
        gmap?.snapshot {
            val totalElapsedTime = SystemClock.elapsedRealtime() - chrono?.base!!
            dbManager.saveRoadStatus(
                RoadStatus(
                    "Scan",
                    it,
                    timerStarted!!,
                    totalElapsedTime,
                    getTravelDistance(),
                    fName
                ), this
            )

        }

    }

    //calculates total distance traveled
    private fun getTravelDistance(): Float {
        return Utilities.calculateTotalDistance(polyline!!)
    }

    //function that starts a thread to add data to array of data
    private fun gotData() {
        GlobalScope.launch {
            addData()
        }

    }

    //updates the speed with the current speed and updates the textview made for it
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                speed = if (loc!!.hasSpeed()) round(loc!!.speed * 3.6).toFloat() else 0f
                speedText?.text = "$speed KM/H"
                delay(500)
            }
        }
    }

    //adds data from sensors to the array of data
    private suspend fun addData() {
        withContext(Dispatchers.Default) {

            endFile += "$index,$speed,${accData[0]},${accData[1]},${accData[2]},${gyroData[0]},${gyroData[1]},${gyroData[2]},$latitude,$longitude\n"
            index++
        }
    }

    //updates marker of current location on map
    private fun setCurrentPositionMarker() {
        cp?.remove()
        cp = gmap?.addMarker(
            MarkerOptions().position(LatLng(latitude!!, longitude!!)).title("Current Position")
        )
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

    //updates the map with the path that the user took by drawing a polyline
    private fun setPolyLineOnMap() {
        trajectoryPolyLine?.remove()
        trajectoryPolyLine = gmap?.addPolyline(polyline)

    }

    //function to get the devices' current location only once
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
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


    override fun onMapClick(p0: LatLng?) {
    }

    //when long clicked on the map , marks the touch position and adds a marker to it , sends a http request to a server which responds with a path to that location from current location
    override fun onMapLongClick(position: LatLng?) {

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

    //function that tells the device to make location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
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


    //makes a http request and gets a response with a json string containing path points
    private fun request(url: String) {
        val queue = Volley.newRequestQueue(this)
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                var jsonObject = JSONObject(response)
                val jsonArray: JSONArray = jsonObject.getJSONArray("routes")
                pathPolyLine = PolylineOptions()
                pathPoints = jsonArray.getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONArray("points")
                for (i in 0 until pathPoints.length()) {
                    var lat = pathPoints.getJSONObject(i).get("latitude") as Double
                    var lon = pathPoints.getJSONObject(i).get("longitude") as Double

                    pathPolyLine?.add(LatLng(lat, lon))
                }
                drawPathPolyline()
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

    //when the path polyline(blue dashed one) is clicked , remove it
    override fun onPolylineClick(p0: Polyline?) {
        if (p0?.tag == "path") {
            pathPolylineOnMap?.remove()
            marker?.remove()
            pathPolylineOnMap = null
        }
    }


}


