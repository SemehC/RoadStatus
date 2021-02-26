package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.location.Location
import android.location.LocationListener
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.*
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round


@Suppress("DEPRECATION")
class SamplingActivity : AppCompatActivity(), GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener {

    private val dbmanager by lazy {
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
    var isRecording = false
    var acc_sensor: Sensor? = null
    var gyro: Sensor? = null
    var sensorManager: SensorManager? = null
    var loc: Location? = null
    var speed: Float = 0f
    var index: Int = 0
    var endFile: String=""
    var map = mutableMapOf<Int, Map<String, String>>()

    var gmap: GoogleMap? = null
    var polyline: PolylineOptions? = PolylineOptions()
    var longitude: Double? = 0.0
    var altitude: Double? = 0.0
    var latitude: Double? = 0.0

    var timerStarted: Long? = 0L
    var timer: Long = 0L
    var speedText: TextView? = null
    var timeText: TextView? = null

    var samlplingDelay: Float = 1000F

    var gManager: GyroscopeListener = GyroscopeListener()
    var accManager: AccelerometerListener = AccelerometerListener()

    var locationManager: LocationManager? = null
    var locationObtained: Boolean = false

    var stillScanning: Boolean = true

    var appFolderPath: String? = null
    lateinit var folderName: String
    var appFolder: File? = null
    var filesFolder: File? = null
    var cameraIsOpened = false
    var recordNumber: Int = 0
    val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
    }
    val imageButton: ImageButton by lazy {
        findViewById(R.id.imageButton2)
    }
    val startStopRecording: ImageButton by lazy {
        findViewById(R.id.recordVideoButton)
    }
    val openCameraButton: ImageButton by lazy {
        findViewById(R.id.cameraButton)
    }


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
        mapView.isClickable=true
        mapView.getMapAsync {
            gmap = it
            gmap?.setOnMapClickListener(this)
            gmap?.setOnMapLongClickListener(this)
            gmap?.setOnCameraIdleListener(this)
        }



        // Getting location manager
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)

        //Getting Sensors Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acc_sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        //Stop Button Clicked !!
        stopButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default) {
                if (isRecording) {
                    stopRecording()
                    isRecording = false
                }
                saveFile()
            }
            stillScanning = false
            startActivity(Intent(this, MainActivity::class.java))

        }

        //Take a picture
        imageButton.setOnClickListener {
            capturePicture()
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
                    imageButton.isVisible = false
                    imageButton.isEnabled = false
                    imageButton.isClickable = false
                    openCameraButton.setImageResource(android.R.drawable.presence_video_online)
                    Toast.makeText(this, "Camera closed !", Toast.LENGTH_SHORT).show()
                } else {
                    videoPreview.isVisible = true
                    connectCamera()
                    startStopRecording.isEnabled = true
                    startStopRecording.isClickable = true
                    startStopRecording.isVisible = true
                    imageButton.isVisible = true
                    imageButton.isEnabled = true
                    imageButton.isClickable = true
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


    private fun initUI() {
        imageButton.isVisible = false
        imageButton.isEnabled = false
        imageButton.isClickable = false
        startStopRecording.isVisible = false
        startStopRecording.isEnabled = false
        startStopRecording.isClickable = false
    }

    private fun capturePicture() {

    }

    //Get camera state (opened / disconnected / error)
    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice) {


            Log.d(TAG, "camera device opened")
            cameraDevice = p0
            previewSession()

        }

        override fun onDisconnected(p0: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            p0.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {
            Log.d(TAG, "camera device disconnected")
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
            Log.d(TAG, "width: $p1 height: $p2")
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

        saveToDatabase(folderName)

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

        sensorManager!!.registerListener(accManager, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(gManager, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        startBackgroundThread()
        if (videoPreview.isAvailable)
        else
            videoPreview.surfaceTextureListener = surfaceListener

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

    var oldtime: Long = 0
    var x = 0f

    @SuppressLint("MissingPermission")

    lateinit var gyroData: Array<Double>
    lateinit var accData: Array<Double>
    private suspend fun scanning() {

        while (stillScanning) {
            timer = System.currentTimeMillis() - timerStarted!!
            var currentTime = System.currentTimeMillis()
            if ((currentTime - oldtime) > samlplingDelay) {
                oldtime = currentTime
                gyroData = gManager.getData()
                accData = accManager.getData()
                gotData()
                delay(500)
            }

        }
    }


    private fun saveToDatabase(fname:String){
        val r = RoadStatus()
        r.total_time = timer
        r.date = timerStarted!!
        r.file_name=fname
        dbmanager.saveRoadStatus(r,this)
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
                if (speed < 9) samlplingDelay = 1000f
                if (speed >= 9) samlplingDelay = 500f
                if (speed > 16) samlplingDelay = 250f
                if (speed > 27) samlplingDelay = 100f
            } else {
                speed = 0f
                samlplingDelay = 5000f
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
            var array = mapOf("speed" to speed * 3.6, "Gyro-x" to gyroData[0], "Gyro-y" to gyroData[1], "Gyro-z" to gyroData[2], "Acc-x" to accData[0], "Acc-y" to accData[1], "Acc-z" to accData[2], "Longitude" to longitude, "Latitude" to latitude, "Altitude" to altitude)
            map[index] = array as Map<String, String>
            index++

            polyline?.add(LatLng(latitude!!, longitude!!))
        }
    }


    private fun updateMapUI() {
        gmap?.clear()
        gmap?.clear()
        gmap?.addMarker(
                MarkerOptions().position(LatLng(latitude!!, longitude!!))
                        .title("My Position")
        )
        gmap?.addPolyline(polyline)
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude!!, longitude!!), 20.0f))
    }

    private fun startTimer() {
        timerStarted = System.currentTimeMillis()
    }

    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            loc = location
            longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
            altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
            latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
            if (longitude != 0.0 && latitude != 0.0 && !locationObtained) {
                locationObtained = true
                startScanning()
                updateMapUI()
                startTimer()
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

    }

    override fun onMapClick(p0: LatLng?) {
        Toast.makeText(this,"Got location : [${p0.latitude},${p0.longitude}",Toast.LENGTH_SHORT).show()
    }

    override fun onMapLongClick(p0: LatLng?) {

    }

    override fun onCameraIdle() {

    }




}


