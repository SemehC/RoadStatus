package tn.enis.roadstatus

import android.location.Location
import android.os.Bundle
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_exploring.*
import kotlinx.android.synthetic.main.activity_scanning.*
import kotlinx.android.synthetic.main.activity_scanning.mapView
import org.json.JSONObject
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.RoadStatistics
import tn.enis.roadstatus.other.ScanStatistics
import java.io.File
import java.nio.ByteBuffer


class Exploring : MapFeatures(true, true) {
    private var allRoadsStatistics: ArrayList<ScanStatistics> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_exploring)
        super.onCreate(savedInstanceState)
        getDataFromDataBase()
    }

    override fun gotData() {
        predictRoadQuality(floatArrayOf(accData[0].toFloat(),accData[1].toFloat(),accData[2].toFloat()))
    }

    private fun getDataFromDataBase() {
        val appFolderPath = this.getExternalFilesDir(null)?.absolutePath

        val appFolder = File(appFolderPath, "PFA")

        val allRoads = DatabaseHandler().getAllRoadStatus(this)
        allRoads?.forEach {
            val folderName = it.file_name
            val data = File(appFolder.absolutePath + "/" + folderName + "/data.json")
            val roadStatusData = JSONObject(data.readLines().joinToString())
            val scanStatistics = ScanStatistics(ArrayList())

            for (i in 1 until roadStatusData.length()) {
                val long = roadStatusData.getJSONObject(i.toString()).get("Longitude") as Double
                val lat = roadStatusData.getJSONObject(i.toString()).get("Latitude") as Double

                val accX = roadStatusData.getJSONObject(i.toString()).get("Acc-x") as Double
                val accY = roadStatusData.getJSONObject(i.toString()).get("Acc-y") as Double
                val accZ = roadStatusData.getJSONObject(i.toString()).get("Acc-z") as Double

                val gyroX = roadStatusData.getJSONObject(i.toString()).get("Gyro-x") as Double
                val gyroY = roadStatusData.getJSONObject(i.toString()).get("Gyro-y") as Double
                val gyroZ = roadStatusData.getJSONObject(i.toString()).get("Gyro-z") as Double

                val speed = roadStatusData.getJSONObject(i.toString()).get("speed") as Double

                val roadStat = RoadStatistics(
                    LatLng(lat, long),
                    arrayOf(accX, accY, accZ),
                    arrayOf(gyroX, gyroY, gyroZ),
                    speed
                )

                scanStatistics.roadsStatistics.add(roadStat)

            }
            allRoadsStatistics.add(scanStatistics)

        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdates()
    }

    override fun onMapLoaded() {
        getDeviceLocation()
        startLocationUpdates()
    }
}