package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class SamplingActivity : AppCompatActivity() {
    var acc_sensor: Sensor? = null
    var gyro: Sensor? = null
    var sensorManager: SensorManager? = null
    var loc: Location? = null
    var speed: Float = 0f
    var index: Int = 0
    var endFile: String? = null
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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        //Initializing Index and the end file
        index = 0
        endFile = ""
        //Initializing Location Manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)
        timeText = findViewById(R.id.time_text_view)
        //Getting Google Map
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            gmap = it
        }


        // Getting location manager
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)

        //Getting Sensors Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acc_sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        //Stop Button Clicked !!
        val bt = findViewById<Button>(R.id.stop_scan_bt)
        bt.setOnClickListener {
            saveFile()
            stillScanning = false
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


    }


    private fun saveFile() {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        var filename: String = simpleDateFormat.format(Date()) + ".json"
        val folder = this.getExternalFilesDir(null)?.absolutePath
        val f = File(folder, "PFA")
        val file = File(f.absolutePath + "/" + filename)
        if (!f.exists()) {
            f.mkdir()
        } else {
            try {
                val gson = Gson()
                endFile += gson.toJson(map)
                val fw = FileWriter(file.absoluteFile)
                val bw = BufferedWriter(fw)
                bw.write(endFile.toString())
                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
                System.exit(-1)
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


    }

    override fun onPause() {
        super.onPause()
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
            speedText?.text = (speed * 3.6).toString() + " KM/H"
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


}


