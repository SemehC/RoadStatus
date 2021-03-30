package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.TextureView
import android.widget.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.*
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import tn.enis.roadstatus.listeners.AccelerometerListener
import tn.enis.roadstatus.listeners.GyroscopeListener
import tn.enis.roadstatus.other.Constants.GPS_ACCURACY
import tn.enis.roadstatus.other.Utilities
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class SamplingActivity : MapFeatures() {


    private val dbManager by lazy {
        DatabaseHandler()
    }


    private var accSensor: Sensor? = null
    private var gyro: Sensor? = null
    private var sensorManager: SensorManager? = null

    private var index: Int = 0
    private var endFile1: String = ""
    private var endFile2: String =
        "Id,Speed,Accelerometer_x,Accelerometer_y,Accelerometer_z,Gyroscope_x,Gyroscope_y,Gyroscope_z,RoadType,RoadQuality\n"
    private var map = mutableMapOf<Int, Map<String, String>>()
    private var gManager: GyroscopeListener = GyroscopeListener()
    private var accManager: AccelerometerListener = AccelerometerListener()

    private var stillScanning: Boolean = true

    private var appFolderPath: String? = null
    private lateinit var folderName: String
    private var appFolder: File? = null
    private var filesFolder: File? = null

    private val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
    }

    private lateinit var gyroData: Array<Double>
    private lateinit var accData: Array<Double>
    private var deviceCameraManager: DeviceCameraManager? = null

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            deviceCameraManager!!.connectCamera()
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit

    }

    private var roadType:String?=null
    private var roadQuality:String?=null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        settings.context = this
        roadType=intent.getStringExtra("road type")
        roadQuality=intent.getStringExtra("road quality")
        println("road quality : $roadQuality")
        println("road type : $roadType")
        settings.loadSettings()
        createFolders()
        //Initializing Camera Manager
        deviceCameraManager = DeviceCameraManager(filesFolder!!, this, videoPreview)

        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)
        chrono = findViewById(R.id.time_text_view)
        deviceCameraManager!!.startBackgroundThread()
        videoPreview.surfaceTextureListener = surfaceListener
        //Getting Google Map
        mapView.onCreate(savedInstanceState)
        mapView.isClickable = true

        mapView.getMapAsync {
            initializeGoogleMap(it)
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
                deviceCameraManager!!.stopRecording()
                saveFile()
            }
            stillScanning = false
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    //request location updates and update the ui with new location aswell as register accelerometer and gyroscope data
    override fun updateLocation() {
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.smallestDisplacement = (settings.distanceBetweenPoints.toFloat() / 10)
        println("distance = ${locationRequest?.smallestDisplacement}")
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


    //creates folders inside the app's folder , with its name being the current date
    @SuppressLint("SimpleDateFormat")
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

    //writes the data acquired by the sensors to a json file && csv file
    private suspend fun saveFile() {
        val file1 = File(filesFolder!!.absolutePath + "/data.json")
        val file2 = File(filesFolder!!.absolutePath + "/data.csv")
        withContext(Dispatchers.IO) {
            while (!file1.exists() && !file2.exists()) {

                try {
                    val fw1 = FileWriter(file1.absoluteFile)
                    val fw2 = FileWriter(file2.absoluteFile)
                    val bw1 = BufferedWriter(fw1)
                    val bw2 = BufferedWriter(fw2)
                    val gson = Gson()
                    endFile1 += gson.toJson(map)
                    bw1.write(endFile1)
                    bw2.write(endFile2)
                    bw1.flush()
                    bw2.flush()
                    bw1.close()
                    bw2.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    exitProcess(-1)
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        sensorManager!!.registerListener(accManager, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(gManager, gyro, SensorManager.SENSOR_DELAY_NORMAL)


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


    //adds data from sensors to the array of data
    private suspend fun addData() {
        withContext(Dispatchers.Default) {
            val array = mapOf(
                "speed" to speed,
                "Gyro-x" to gyroData[0],
                "Gyro-y" to gyroData[1],
                "Gyro-z" to gyroData[2],
                "Acc-x" to accData[0],
                "Acc-y" to accData[1],
                "Acc-z" to accData[2],
                "Longitude" to longitude,
                "Latitude" to latitude,
                "Altitude" to altitude
            )
            map[index] = array as Map<String, String>
            endFile2 += "$index,$speed,${accData[0]},${accData[1]},${accData[2]},${gyroData[0]},${gyroData[1]},${gyroData[2]},$roadType,$roadQuality\n"
            index++
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Back button disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onMapLoaded() {
        getDeviceLocation()
        startLocationUpdates()
        deviceCameraManager!!.recordSession()
    }

}


