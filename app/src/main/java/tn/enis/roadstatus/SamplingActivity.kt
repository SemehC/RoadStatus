package tn.enis.roadstatus

import  android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round


@Suppress("DEPRECATION")
class SamplingActivity : AppCompatActivity(), GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback {

    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var marker: Marker? = null
    private val dbManager by lazy {
        DatabaseHandler()
    }

    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private val mediaRecorder by lazy {
        MediaRecorder()
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
    private var p: Polyline? = null


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
    private var recordNumber: Int = 0
    private val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)


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
                    stopRecording()
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
                stopRecording()
                previewSession()
                startStopRecording.setImageResource(android.R.drawable.presence_online)
                Toast.makeText(this, "Stopped recording !", Toast.LENGTH_SHORT).show()
            } else {
                recordSession()
                startStopRecording.setImageResource(android.R.drawable.ic_notification_overlay)
                Toast.makeText(this, "Started recording !", Toast.LENGTH_SHORT).show()
            }
            isRecording = !isRecording
        }

        //Open or close the camera
        openCameraButton.setOnClickListener {
            if (!isRecording) {
                if (cameraIsOpened) {
                    closeCamera()
                    videoPreview.isVisible = false
                    startStopRecording.isEnabled = false
                    startStopRecording.isClickable = false
                    startStopRecording.isVisible = false

                    openCameraButton.setImageResource(android.R.drawable.presence_video_online)
                    Toast.makeText(this, "Camera closed !", Toast.LENGTH_SHORT).show()
                } else {
                    videoPreview.isVisible = true
                    connectCamera()
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
        createFolders()
    }
    var prevLocation:Location?=null

    private fun updateLocation() {
        val distanceBetweenPositions:FloatArray?=null
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.interval = 50
        locationRequest?.fastestInterval = 10
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                loc = locationResult.locations.last()
                if(prevLocation==null){
                    prevLocation=loc
                    longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                    altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                    latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                    speed = if (loc!!.hasSpeed()) (loc!!.speed*3.6).toFloat() else 0f
                    polyline?.add(LatLng(latitude!!, longitude!!))
                }
                else{
                    try {
                        Location.distanceBetween(prevLocation!!.latitude,prevLocation!!.longitude,loc!!.latitude,loc!!.longitude,distanceBetweenPositions)
                    }catch(e:Exception) {
                        distanceBetweenPositions?.set(0, 0f)
                    }
                    if(distanceBetweenPositions!=null){
                        println("Distance between positions : "+distanceBetweenPositions?.get(0))
                        if(distanceBetweenPositions?.get(0)!! >20f){
                            setCurrentPositionMarker()
                            longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                            altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                            latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                            speed = if (loc!!.hasSpeed()) (loc!!.speed*3.6).toFloat() else 0f
                            polyline?.add(LatLng(latitude!!, longitude!!))
                            prevLocation=loc
                        }
                    }
                }
                setCurrentPositionMarker()
                setPolyLineOnMap()

            }
        }
    }


    private fun initUI() {

        startStopRecording.isVisible = false
        startStopRecording.isEnabled = false
        startStopRecording.isClickable = false
    }

    private fun capturePicture() {

    }

    //Get camera state (opened / disconnected / error)
    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice) {



            cameraDevice = p0
            previewSession()

        }

        override fun onDisconnected(p0: CameraDevice) {

            p0.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {

            finish()
        }

    }

    // Record the video output taken by camera
    private fun recordSession() {

        setupMediaRecorder()

        val surfaceTexture = videoPreview.surfaceTexture
        val textureSurface = Surface(surfaceTexture)
        val recordSurface = mediaRecorder.surface

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(recordSurface)
        val surfaces = arrayListOf<Surface>().apply {
            add(textureSurface)
            add(recordSurface)
        }

        cameraDevice.createCaptureSession(surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "creating record session failed!")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)

                        mediaRecorder.start()
                    }

                }, backgroundHandler)
    }

    //Method to preview the camera output on the "Box" designated for it
    private fun previewSession() {
        try {

            val surfaceTexture = videoPreview.surfaceTexture
            val surface = Surface(surfaceTexture)
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            cameraDevice.createCaptureSession(mutableListOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Failed to create capture session")
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, e.toString())
                            }
                        }
                    }, null)

        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun closeCamera() {
        videoPreview.isVisible = false
        if (this::captureSession.isInitialized)
            captureSession.close()
        if (this::cameraDevice.isInitialized)
            cameraDevice.close()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camara2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()

        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    companion object {
        private val TAG = SamplingActivity::class.qualifiedName
    }

    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)!!
            else -> throw  IllegalArgumentException("Key not recognized")
        }
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {

        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit

    }

    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter { lens == cameraCharacteristics(it, CameraCharacteristics.LENS_FACING) }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)
        Log.e(TAG, "deviceId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
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

    @Throws(IOException::class)
    protected fun setupMediaRecorder() {
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(filesFolder!!.absolutePath + "/Recording$recordNumber.mp4")
            setVideoEncodingBitRate(1000000)
            setVideoFrameRate(30)
            setVideoSize(1280, 720)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            prepare()
            recordNumber++
        }
    }

    private fun stopRecording() {
        mediaRecorder.apply {
            stop()
            reset()
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
                    bw.write(endFile.toString())
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
        startBackgroundThread()
        if (videoPreview.isAvailable)
        else
            videoPreview.surfaceTextureListener = surfaceListener
        startLocationUpdates()
    }

    override fun onPause() {

        super.onPause()
        closeCamera()
        stopBackgroundThread()
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

    lateinit var gyroData: Array<Double>
    lateinit var accData: Array<Double>
    private suspend fun scanning() {

        while (stillScanning) {

            timer = System.currentTimeMillis() - timerStarted!!
            gyroData = gManager.getData()
            accData = accManager.getData()
            gotData()
            delay(samlplingDelay)
        }
    }

    private fun saveToDatabase(fname: String) {


        gmap?.snapshot {
            dbManager.saveRoadStatus(RoadStatus("Scan", it, timerStarted!!, timer, 69f, fname), this)

        }

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


    private fun setCurrentPositionMarker(){
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

    var trajectoryPolyLine:Polyline?=null
    private fun setPolyLineOnMap(){
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
                    updateMapUI()
                    startTimer()
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
                    p?.remove()
                    p = gmap?.addPolyline(pathPolyLine)
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
    }

}


