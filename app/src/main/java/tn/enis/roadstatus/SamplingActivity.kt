package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_exploring.*
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.android.synthetic.main.activity_scanning.mapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import tn.enis.roadstatus.listeners.AccelerometerListener
import tn.enis.roadstatus.listeners.GyroscopeListener
import tn.enis.roadstatus.other.Constants.GPS_ACCURACY
import tn.enis.roadstatus.other.Utilities
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

@SuppressLint
class SamplingActivity : MapFeatures(true, false) {
    private val dbManager by lazy {
        DatabaseHandler()
    }

    private var index: Int = 0
    private var endFile1: String = ""
    private var endFile2: String =
        "Id,Speed,Accelerometer_x,Accelerometer_y,Accelerometer_z,Gyroscope_x,Gyroscope_y,Gyroscope_z,RoadType,RoadQuality\n"
    private var map = mutableMapOf<Int, Map<String, String>>()

    private var stillScanning: Boolean = true

    private val stopButton: Button by lazy {
        findViewById(R.id.stop_scan_bt)
    }


    private var roadType: String? = null
    private var roadQuality: String? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_scanning)
        super.onCreate(savedInstanceState)
        roadType = intent.getStringExtra("road type")

        roadQuality = intent.getStringExtra("road quality")
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
    override fun gotData() {
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


