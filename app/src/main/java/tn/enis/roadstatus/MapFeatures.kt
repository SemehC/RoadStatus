package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.TextureView
import android.widget.Chronometer
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_scanning.mapView
import kotlinx.android.synthetic.main.activity_scanning.videoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import tn.enis.roadstatus.listeners.AccelerometerListener
import tn.enis.roadstatus.listeners.GyroscopeListener
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Settings
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round

@Suppress("DEPRECATION")
abstract class MapFeatures(
    protected var usingCamera: Boolean = false,
    protected var realTimeClassification: Boolean = false
) : AppCompatActivity(), GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnPolylineClickListener {
    private var accSensor: Sensor? = null
    private var gyro: Sensor? = null
    private var sensorManager: SensorManager? = null
    private var trajectoryPolyLine: Polyline? = null
    private val pattern = listOf(Dot(), Gap(20F), Dash(30F), Gap(20F))
    private var marker: Marker? = null
    private var startingPosition: LatLng? = null
    private var initialMarker: Marker? = null
    private var cp: Marker? = null
    private var url: String? = null
    private var pathPolyLine: PolylineOptions? = PolylineOptions()
    private val imageInterpreter by lazy {
        loadModel("model")?.let { Interpreter(it) }
    }
    private val roadQualityInterpreter by lazy {
        loadModel("roadPrediction")?.let { Interpreter(it) }
    }
    private var imageLabels = arrayOf("Bituminous", "Concrete", "Earthen", "WBM")
    private var roadQualityLabels = arrayOf("Bad", "Average", "Good")
    private var imagePredictions: IntArray = IntArray(4)
    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            if (usingCamera)
                deviceCameraManager!!.connectCamera()
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            //Predict the image each second has passed
            currentTime = System.currentTimeMillis()
            //One second has passsed
            if (currentTime - timeStarted >= 1000 && usingCamera && realTimeClassification) {
                videoPreview.bitmap?.let { it1 -> predictImage(it1) }
                timeStarted = System.currentTimeMillis()
            }
        }
    }
    // Our model expects a RGB image, hence the channel size is 3
    private val channelSize = 3
    // Width of the image that our model expects
    private var inputImageWidth = 224
    // Height of the image that our model expects
    private var inputImageHeight = 224
    // Size of the input buffer size (if your model expects a float input, multiply this with 4)
    private var modelInputSize = inputImageWidth * inputImageHeight * channelSize
    // Output you get from your model, this is essentially as we saw in netron
    private var imageClassificationResultArray = Array(1) { ByteArray(4) }
    private var roadClassificationResultArray = Array(1) { FloatArray(3) }
    private var pathPolylineOnMap: Polyline? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val satelliteStyleButton: RadioButton by lazy {
        findViewById(R.id.satelliteStyle)
    }
    private val mapStyleButton: RadioButton by lazy {
        findViewById(R.id.mapStyle)
    }
    private var speedText: TextView? = null
    private val settings = Settings()
    private var appFolderPath: String? = null
    private var appFolder: File? = null
    private lateinit var pathPoints: JSONArray
    protected var polyline: PolylineOptions? = PolylineOptions()
    protected var timeStarted = System.currentTimeMillis()
    protected var currentTime: Long = 0
    protected var gmap: GoogleMap? = null
    protected var longitude: Double? = 0.0
    protected var altitude: Double? = 0.0
    protected var latitude: Double? = 0.0
    protected var loc: Location? = null
    protected var timerStarted: Long? = 0L
    protected var chrono: Chronometer? = null
    protected var speed: Float = 0f
    protected var filesFolder: File? = File("")
    protected var gManager: GyroscopeListener = GyroscopeListener()
    protected var accManager: AccelerometerListener = AccelerometerListener()
    protected var deviceCameraManager: DeviceCameraManager? = null
    protected lateinit var gyroData: Array<Double>
    protected lateinit var accData: Array<Double>
    protected lateinit var folderName: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings.context = this
        settings.loadSettings()

        mapView.getMapAsync {
            initializeGoogleMap(it)
        }
        //Getting Google Map
        mapView.onCreate(savedInstanceState)
        mapView.isClickable = true

        satelliteStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        mapStyleButton.setOnClickListener {
            gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        //Getting Text views
        speedText = findViewById(R.id.speed_text_view)
        chrono = findViewById(R.id.time_text_view)
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        updateLocation()
        //Getting Sensors Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        videoPreview.surfaceTextureListener = surfaceListener
        if (!realTimeClassification) {
            createFolders()
        }
        deviceCameraManager = DeviceCameraManager(filesFolder!!, this, videoPreview)
    }

    fun predictRoadQuality(modelInput: FloatArray) {
        // Perform inference on the model
        roadQualityInterpreter?.run(modelInput, roadClassificationResultArray)
        val predictions = roadClassificationResultArray[0]
        val index = predictions.indexOfFirst { it == predictions.maxOrNull()!! }
        println(roadQualityLabels[index])
    }

    fun predictImage(bitmap: Bitmap) {

        // Resize the bitmap so that it's 224x224
        val resizedImage = bitmap.let { it1 ->
            Bitmap.createScaledBitmap(
                it1,
                inputImageWidth,
                inputImageHeight,
                true
            )
        }

        // Convert the bitmap to a ByteBuffer
        val modelInput = resizedImage?.let { it1 -> convertBitmapToByteBuffer(it1) }

        // Perform inference on the model
        imageInterpreter?.run(modelInput, imageClassificationResultArray)
        val predictionBytes = imageClassificationResultArray[0]
        for (i in predictionBytes.indices) {
            imagePredictions[i] = abs(predictionBytes[i].toInt())
        }
        val index = imagePredictions.indexOf(imagePredictions.maxOrNull()!!)
        println(imageLabels[index])
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Specify the size of the byteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        // Calculate the number of pixels in the image
        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        // Loop through all the pixels and save them into the buffer
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val pixelVal = pixels[pixel++]
                // Do note that the method to add pixels to byteBuffer is different for quantized models over normal tflite models
                byteBuffer.put((pixelVal shr 16 and 0xFF).toByte())
                byteBuffer.put((pixelVal shr 8 and 0xFF).toByte())
                byteBuffer.put((pixelVal and 0xFF).toByte())
            }
        }

        // Recycle the bitmap to save memory
        bitmap.recycle()
        return byteBuffer
    }

    private fun loadModel(name: String): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = assets.openFd("$name.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun initializeGoogleMap(googleMap: GoogleMap) {
        gmap = googleMap
        gmap?.setOnMapClickListener(this)
        gmap?.setOnMapLongClickListener(this)
        gmap?.setOnCameraIdleListener(this)
        gmap?.setOnMapLoadedCallback(this)
        gmap?.setOnPolylineClickListener(this)
        gmap?.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    //request location updates and update the ui with new location aswell as register accelerometer and gyroscope data
    private fun updateLocation() {
        locationRequest = LocationRequest.create()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.smallestDisplacement = (settings.distanceBetweenPoints.toFloat() / 10)
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

                        gyroData = gManager.getData()
                        accData = accManager.getData()
                        gotData()

                        polyline?.add(LatLng(latitude!!, longitude!!))

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
                        setPolyLineOnMap()
                    }
                }
            }
        }
    }

    abstract fun gotData()

    //function to update the auto generated path when user is close to one of its points by removing it and redrawing the polyline
    private fun checkNavigationPath() {

        if (pathPolyLine != null) {
            if (pathPolyLine?.points?.size!! > 0) {
                val pathPolylineNextPointLocation = Location("")
                val newLine = PolylineOptions()
                for (i in 0 until pathPolyLine?.points?.size!!) {
                    newLine.add(
                        LatLng(
                            pathPolyLine?.points?.get(i)?.latitude!!,
                            pathPolyLine?.points?.get(i)?.longitude!!
                        )
                    )
                }

                for (i in 0 until pathPolyLine?.points?.size!!) {
                    if (newLine.points.size > 0) {
                        if (newLine.points.indexOf(pathPolyLine?.points?.get(i)) != -1) {
                            val pos =
                                newLine.points[newLine.points.indexOf(pathPolyLine?.points?.get(i))]

                            pathPolylineNextPointLocation.latitude = pos.latitude
                            pathPolylineNextPointLocation.longitude = pos.longitude

                            if (loc?.distanceTo(pathPolylineNextPointLocation)!! > Constants.MIN_DISTANCE_TO_REMOVE_PT) {
                                newLine.points.removeAt(
                                    newLine.points.indexOf(
                                        pathPolyLine?.points?.get(
                                            i
                                        )
                                    )
                                )
                            }
                        }
                    }

                }
                pathPolyLine = newLine
                drawPathPolyline()
            }
        }
    }

    //draws a polyline which leads to a point marker on the map with a long click
    private fun drawPathPolyline() {
        pathPolylineOnMap?.remove()
        pathPolyLine?.color(Color.BLUE)
        pathPolylineOnMap = gmap?.addPolyline(pathPolyLine)
        pathPolylineOnMap?.tag = "path"
        pathPolylineOnMap?.isClickable = true
        pathPolylineOnMap?.pattern = pattern
        pathPolylineOnMap?.width = 20f
    }

    //updates the map with the path that the user took by drawing a polyline
    fun setPolyLineOnMap() {
        trajectoryPolyLine?.remove()
        trajectoryPolyLine = gmap?.addPolyline(polyline)

    }

    //function that tells the device to make location updates
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun removePredictedPathPolyline(polyline: Polyline?) {
        if (polyline?.tag == "path") {
            pathPolylineOnMap?.remove()
            marker?.remove()
            pathPolylineOnMap = null
        }
    }

    //creates folders inside the app's folder , with its name being the current date
    @SuppressLint("SimpleDateFormat")
    fun createFolders() {
        appFolderPath = this.getExternalFilesDir(null)?.absolutePath
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        folderName = simpleDateFormat.format(Date())
        appFolder = File(appFolderPath, "PFA")
        filesFolder = File(appFolder!!.absolutePath, folderName)
        if (!appFolder?.exists()!!) {
            appFolder?.mkdir()
        }
        if (!filesFolder!!.exists()) {
            filesFolder!!.mkdir()
        }

    }

    override fun onResume() {
        super.onResume()
        deviceCameraManager!!.startBackgroundThread()
        mapView.onResume()
        sensorManager!!.registerListener(accManager, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(gManager, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        startLocationUpdates()

    }

    //function to get the devices' current location only once
    @SuppressLint("MissingPermission")
    fun getDeviceLocation() {
        fusedLocationProviderClient?.flushLocations()
        startingPosition = null
        val locationResult = fusedLocationProviderClient?.lastLocation
        locationResult?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                if (task.result != null) {
                    loc = task.result

                    longitude = if (loc?.longitude == null) 0.0 else loc?.longitude
                    altitude = if (loc?.altitude == null) 0.0 else loc?.altitude
                    latitude = if (loc?.latitude == null) 0.0 else loc?.latitude
                    startingPosition = LatLng(latitude!!, longitude!!)

                    timerStarted = System.currentTimeMillis()
                    chrono?.base = SystemClock.elapsedRealtime()
                    chrono?.start()
                    updateUI()
                    speed = 0f
                    setInitialPositionMarker()
                    polyline?.add(startingPosition)

                    // Set the map's camera position to the current location of the device.
                    gmap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                loc!!.latitude,
                                loc!!.longitude
                            ), 20f
                        )
                    )

                }
            }
        }
    }

    //updates the speed with the current speed and updates the textview made for it
    @SuppressLint("SetTextI18n")
    fun updateUI() {
        GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                speed = if (loc!!.hasSpeed()) round(loc!!.speed * 3.6).toFloat() else 0f
                speedText?.text = "$speed KM/H"
                delay(500)
            }
        }
    }

    private fun setInitialPositionMarker() {
        if (initialMarker == null) {
            initialMarker = gmap?.addMarker(
                startingPosition?.let {
                    MarkerOptions().position(it)
                        .title("Initial Position")
                }
            )
        }
    }

    fun setCurrentPositionMarker() {
        cp?.remove()
        cp = gmap?.addMarker(
            MarkerOptions().position(LatLng(latitude!!, longitude!!)).title("Current Position")
        )
    }

    private fun requestDestinationPolyline(position: LatLng?) {
        url = "https://api.tomtom.com/routing/1/calculateRoute/"
        marker?.remove()
        marker = gmap?.addMarker(
            MarkerOptions().position(position!!)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        url += "${loc?.latitude},${loc?.longitude}:${position?.latitude},${position?.longitude}/json?key=Vstg8Js5WPgqQJdWwXEyJF3XPzElvdCi"

        request(url!!)
        checkNavigationPath()
    }

    //makes a http request and gets a response with a json string containing path points
    private fun request(url: String) {
        val queue = Volley.newRequestQueue(this)
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                val jsonObject = JSONObject(response)
                val jsonArray: JSONArray = jsonObject.getJSONArray("routes")
                pathPolyLine = PolylineOptions()
                pathPoints = jsonArray.getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONArray("points")
                for (i in 0 until pathPoints.length()) {
                    val lat = pathPoints.getJSONObject(i).get("latitude") as Double
                    val lon = pathPoints.getJSONObject(i).get("longitude") as Double

                    pathPolyLine?.add(LatLng(lat, lon))
                }
                drawPathPolyline()
            },
            {

            })

        // Add the request to the RequestQueue.

        queue.add(stringRequest)

    }

    override fun onPause() {
        super.onPause()
        deviceCameraManager!!.closeCamera()
        deviceCameraManager!!.stopBackgroundThread()
        mapView.onPause()
        sensorManager!!.unregisterListener(accManager)
        sensorManager!!.unregisterListener(gManager)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

    }


    override fun onStop() {
        super.onStop()
        mapView.onStop()
        // Releases model resources if no longer used.
        roadQualityInterpreter?.close()
        imageInterpreter?.close()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onMapClick(p0: LatLng?) {
    }

    override fun onMapLongClick(position: LatLng?) {
        requestDestinationPolyline(position)
    }

    override fun onCameraIdle() {

    }


    override fun onDestroy() {
        gmap?.clear()
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onPolylineClick(p0: Polyline?) {
        removePredictedPathPolyline(p0)
    }

}