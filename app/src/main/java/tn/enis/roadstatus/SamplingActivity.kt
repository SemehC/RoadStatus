package tn.enis.roadstatus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.math.PI


class SamplingActivity : AppCompatActivity(), SensorEventListener {
    var acc_sensor: Sensor? = null
    var gyro: Sensor? = null
    var sensorManager: SensorManager? = null
    var loc:Location?=null
    var speed:Float=0f
    var index:Int = 0
    var endFile:JsonArray?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)
        index = 0
        endFile = JsonArray()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val bt = findViewById<Button>(R.id.stop_scan_bt)
        bt.setOnClickListener {
            val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
            var filename : String = simpleDateFormat.format(Date())
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
                    val fw = FileWriter(file.absoluteFile)
                    val bw = BufferedWriter(fw)
                    bw.write(endFile.toString())
                    bw.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    System.exit(-1)
                }
            }



            startActivity(intent)
        }


        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acc_sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onResume() {
        super.onResume()

        sensorManager!!.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)

    }
    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }

    var oldtime: Long = 0
    var x = 0f
    override fun onSensorChanged(event: SensorEvent?) {

        val sensor = event!!.sensor
        var currentTime = System.currentTimeMillis()
        var gyrox:Double=0.0
        var gyroy:Double=0.0
        var gyroz:Double=0.0
        var accx:Double=0.0
        var accy:Double=0.0
        var accz:Double=0.0
        var longitude:Double?=0.0
        var altitude:Double?=0.0
        var latitude:Double?=0.0
        if ((currentTime - oldtime) > 100) {
            longitude = loc?.longitude
            altitude = loc?.altitude
            latitude = loc?.longitude
            Log.d("Location : ", "" + loc?.longitude + " : " + loc?.latitude + " : " + loc?.altitude)
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
            Log.d("Speed : ", speed.toString())
            var array = mapOf("speed" to speed.toString(), "Gyro-x" to gyrox, "Gyro-y" to gyroy, "Gyro-z" to gyroz, "Acc-x" to accx, "Acc-y" to accy, "Acc-z" to accz, "Longitude" to longitude, "Latitude" to latitude, "Altitude" to altitude)
            var map = mutableMapOf<Int, Map<String, String>>()
            map.put(index, array as Map<String, String>)
            index++
            val gson = Gson()

            //Log.d("JSON : ",gson.toJson(map).toString())
            endFile?.add(gson.toJson(map))
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            //Log.d("Location : ","" + location.longitude + ":" + location.latitude )
            loc = location
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}


