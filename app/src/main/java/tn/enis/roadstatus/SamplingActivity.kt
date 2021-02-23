package tn.enis.roadstatus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_scanning.*
import timber.log.Timber
import tn.enis.roadstatus.other.Utilities
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.random.Random


private const val PERMISSION_REQUEST=10

class SamplingActivity : AppCompatActivity(), SensorEventListener {
    var acc_sensor: Sensor? = null
    var gyro: Sensor? = null
    var sensorManager: SensorManager? = null
    var loc:Location?=null
    var speed:Float=0f
    var index:Int = 0
    var endFile:String?=null
    var map = mutableMapOf<Int, Map<String, String>>()
    var all_permissions_granted=false
    var gmap :GoogleMap?=null
    var polyline: PolylineOptions?= PolylineOptions()
    var longitude:Double?=0.0
    var altitude:Double?=0.0
    var latitude:Double?=0.0


    var speedText:TextView?=null
    var timeText:TextView?=null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        index = 0
        endFile = ""
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)

        speedText = findViewById(R.id.speed_text_view)
        timeText = findViewById(R.id.time_text_view)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            gmap = it
        }

        all_permissions_granted = Utilities.hasLocationPermissions(this) && Utilities.hasStoragePermissions(this)



        val bt = findViewById<Button>(R.id.stop_scan_bt)
        bt.setOnClickListener {

            if(all_permissions_granted){
                val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                var filename : String = simpleDateFormat.format(Date())+".json"
                val folder = this.getExternalFilesDir(null)?.absolutePath
                val f = File(folder, "PFA")
                val file = File(f.absolutePath + "/" + filename)
                if(!f.exists())
                {
                    f.mkdir()
                }
                else
                {
                    try {
                        val gson = Gson()
                        endFile+=gson.toJson(map)
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
            startActivity(Intent(this, MainActivity::class.java))
        }

        if(all_permissions_granted){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            acc_sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        }


    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if(all_permissions_granted){
            sensorManager!!.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager!!.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        }


    }
    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if(all_permissions_granted){
            sensorManager!!.unregisterListener(this)
        }

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
    override fun onSensorChanged(event: SensorEvent?) {

            val sensor = event!!.sensor
            var currentTime = System.currentTimeMillis()
            var gyrox=0.0
            var gyroy=0.0
            var gyroz=0.0
            var accx=0.0
            var accy=0.0
            var accz=0.0

            if ((currentTime - oldtime) > 5000) {

                if(sensor.type == Sensor.TYPE_LINEAR_ACCELERATION){
                    accx = ((event.values[0] * 180) / PI)
                    accy = ((event.values[1] * 180) / PI)
                    accz = ((event.values[2] * 180) / PI)
                }

                if(sensor.type == Sensor.TYPE_GYROSCOPE){
                    gyrox = ((event.values[0] * 180) / PI)
                    gyroy = ((event.values[1] * 180) / PI)
                    gyroz = ((event.values[2] * 180) / PI)
                }

                if(loc?.hasSpeed()==true)
                {
                    speed = loc?.speed!!
                }
                else
                {
                    speed = 0f
                }
                var array = mapOf("speed" to speed, "Gyro-x" to gyrox, "Gyro-y" to gyroy, "Gyro-z" to gyroz, "Acc-x" to accx, "Acc-y" to accy, "Acc-z" to accz, "Longitude" to longitude, "Latitude" to latitude, "Altitude" to altitude)

                map.put(index, array as Map<String, String>)
                index++


            }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    private fun updateMapUI(){
        gmap?.clear()

        var icon = BitmapDescriptorFactory.fromResource(R.drawable.crosshair)


        gmap?.addMarker(
                MarkerOptions().position(LatLng(latitude!!, longitude!!))
                        .title("My Position")
        )
        gmap?.addPolyline(polyline)
    }

    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            loc = location
            longitude = if(loc?.longitude==null) 0.0 else loc?.longitude
            altitude = if(loc?.altitude==null) 0.0 else loc?.altitude
            latitude = if(loc?.latitude==null) 0.0 else loc?.latitude
            gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude!!, longitude!!), 20.0f))
            polyline?.add(LatLng(latitude!!, longitude!!))
            updateMapUI()
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }







}


