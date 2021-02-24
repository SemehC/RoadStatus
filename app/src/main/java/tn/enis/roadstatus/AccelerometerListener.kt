package tn.enis.roadstatus

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.PI

class AccelerometerListener : SensorEventListener {
    private var accx = 0.0
    private var accy = 0.0
    private var accz = 0.0
    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("Event : ", event.toString())
        if (event != null) {
            accx = ((event.values[0] * 180) / PI)
            accy = ((event.values[1] * 180) / PI)
            accz = ((event.values[2] * 180) / PI)
        }

    }

    fun getData(): Array<Double> {
        return arrayOf(accx, accy, accz)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}