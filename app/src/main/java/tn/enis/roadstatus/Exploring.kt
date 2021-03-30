package tn.enis.roadstatus

import android.location.Location
import android.os.Bundle
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_scanning.*
import org.json.JSONObject
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.RoadStatistics
import tn.enis.roadstatus.other.ScanStatistics
import java.io.File


class Exploring : MapFeatures(){

    private var allRoadsStatistics: ArrayList<ScanStatistics> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploring)

        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)

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

        getDataFromDataBase()


        println("Total scans : ${allRoadsStatistics.size}")

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
                    if (loc?.accuracy!! < Constants.GPS_ACCURACY) {
                        setCurrentPositionMarker()
                        longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                        altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                        latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                        speed = if (loc!!.hasSpeed()) (loc!!.speed * 3.6).toFloat() else 0f
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

                        if (pathPolylineOnMap != null) {
                            if (pathPolylineOnMap?.points?.size!! > 0) {
                                val pathPolylineNextPointLocation = Location("")
                                pathPolylineNextPointLocation.latitude =
                                    pathPolylineOnMap?.points?.get(0)!!.latitude
                                pathPolylineNextPointLocation.longitude =
                                    pathPolylineOnMap?.points?.get(0)!!.longitude
                                if (loc?.distanceTo(pathPolylineNextPointLocation)!! < 3f) {
                                    pathPolylineOnMap!!.points.removeAt(0)
                                }
                            }
                        }

                        setPolyLineOnMap()
                        checkNavigationPath()


                    }
                }
            }
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