package tn.enis.roadstatus

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.TextureView
import androidx.core.view.isVisible
import java.io.File
import java.io.IOException


@Suppress("DEPRECATION")
@SuppressLint("LogNotTimber")
class DeviceCameraManager(
    private val filesFolder: File,
    private val context: Context,
    private val videoPreview: TextureView
) {
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession


    private val mediaRecorder by lazy {
        MediaRecorder()
    }
    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var recordNumber: Int = 0


    //Get camera state (opened / disconnected / error)
    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice) {

            cameraDevice = p0
            previewSession()

        }

        override fun onDisconnected(p0: CameraDevice) {
            p0.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {

        }

    }


    // Record the video output taken by camera
    fun recordSession() {
        setupMediaRecorder(filesFolder)
        val surfaceTexture = videoPreview.surfaceTexture
        val textureSurface = Surface(surfaceTexture)
        val recordSurface = mediaRecorder.surface

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(recordSurface)
        val surfaces = arrayListOf<Surface>().apply {
            add(textureSurface)
            add(recordSurface)
        }

        cameraDevice.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating record session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)

                    mediaRecorder.start()
                }

            }, backgroundHandler
        )
    }

    //Method to preview the camera output on the "Box" designated for it
    fun previewSession() {
        try {

            val surfaceTexture = videoPreview.surfaceTexture

            val surface = Surface(surfaceTexture)
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            captureRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(
                mutableListOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to create capture session")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            captureRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            captureSession.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                null
                            )


                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                }, null
            )

        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    fun closeCamera() {
        videoPreview.isVisible = false
        if (this::captureSession.isInitialized)
            captureSession.close()
        if (this::cameraDevice.isInitialized)
            cameraDevice.close()
    }

    //Threads used to get camera's messages
    fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    fun stopBackgroundThread() {
        backgroundThread.quitSafely()

        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    companion object {
        private val TAG = SamplingActivity::class.qualifiedName
    }

    //Get the camera's characteristics (front or back camera)
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)!!
            else -> throw  IllegalArgumentException("Key not recognized")
        }
    }


    //Select which camera to use
    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter {
                lens == cameraCharacteristics(
                    it,
                    CameraCharacteristics.LENS_FACING
                )
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    //Connect to the camera and open it
    @SuppressLint("MissingPermission")
    fun connectCamera() {
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)
        Log.e(TAG, "deviceId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }

    //Setting up the media recorder to capture video
    @Throws(IOException::class)
    fun setupMediaRecorder(filesFolder: File) {
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(filesFolder.absolutePath + "/Recording$recordNumber.mp4")
            setVideoEncodingBitRate(1000000)
            setVideoFrameRate(30)
            setVideoSize(1280, 720)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            prepare()
            recordNumber++
        }
    }

    //stopping the video capture
    fun stopRecording() {
        mediaRecorder.apply {
            stop()
            reset()
        }
    }
}