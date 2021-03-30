package tn.enis.roadstatus.listeners

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class AccelerometerListener : SensorEventListener {
    private var accx = 0.0
    private var accy = 0.0
    private var accz = 0.0
    override fun onSensorChanged(event: SensorEvent?) {

        //Verify values
        if (event != null) {
            accx = event.values[0].toDouble()
            accy = event.values[1].toDouble()
            accz = event.values[2].toDouble()
        }

    }
    fun getData(): Array<Double> {
        return arrayOf(accx, accy, accz)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}