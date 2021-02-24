package tn.enis.roadstatus

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.ArrayMap
import android.util.Log
import kotlin.math.PI

class GyroscopeListener : SensorEventListener {
    private var gyrox = 0.0
    private var gyroy = 0.0
    private var gyroz = 0.0
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            Log.d("Gyroscope", event.values[0].toString())
            gyrox = ((event.values[0] * 180) / PI)
            gyroy = ((event.values[1] * 180) / PI)
            gyroz = ((event.values[2] * 180) / PI)
        }

    }

    fun getData(): Array<Double> {
        return arrayOf(gyrox, gyroy, gyroz)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}