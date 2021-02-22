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
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.PI


class SamplingActivity : AppCompatActivity(), SensorEventListener {
    var acc_sensor: Sensor? = null
    var gyro: Sensor? = null
    var sensorManager: SensorManager? = null
    var loc:Location?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

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
        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0L,0f,locationListener)

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
        if ((currentTime - oldtime) > 100) {

            Log.d("Location : ",""+ loc?.longitude+" : "+loc?.latitude+" : "+loc?.altitude)
            if(sensor.type == Sensor.TYPE_LINEAR_ACCELERATION){
                Log.d("Acc-x: ", event!!.values[0].toString())
                Log.d("Acc-y: ", event!!.values[1].toString())
                Log.d("Acc-z: ", event!!.values[2].toString())
            }

            if(sensor.type == Sensor.TYPE_GYROSCOPE){
                Log.d("Gyr-x: ", ((event!!.values[0] * 180) / PI).toString())
                Log.d("Gyr-y: ", ((event!!.values[1] * 180) / PI).toString())
                Log.d("Gyr-z: ", ((event!!.values[2] * 180) / PI).toString())
            }

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