package dev.alejandrorosas.streamlib

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.base.MyLog
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.view.OpenGlView
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Arrays

class StreamService : Service() {
    companion object {
        private const val TAG = "StreamService"
        private const val channelId = "rtpStreamChannel"
        private const val notifyId = 123456

        var openGlView: OpenGlView? = null

        var usbDeviceStatus: Int = -1
        var streamStatus: Int = -1
        var visitId: String = ""
        var caseId: String? = ""

        // 可选分辨率列表
        val resolutions = mutableListOf<CharSequence>(
            "800x600",
            "1280x720",
            "1920x1080",
        )
    }


    val isStreaming: Boolean get() = endpoint != null
    var cameraWidth = 1280
    var cameraHeight = 720

    private var endpoint: String? = null
    private var rtmpUSB: RtmpUSB? = null
    private var uvcCamera: UVCCamera? = null
    private var usbMonitor: USBMonitor? = null
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }


    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onCreate() {
        super.onCreate()
         MyLog.e("$TAG #onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        keepAliveTrick()
    }

    private fun keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId).setOngoing(true).setContentTitle("").setContentText("").build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         MyLog.e("$TAG #onStartCommand")

        intent?.getStringExtra("resolution")?.let {
            val (w, h) = it.split("x").map { it -> it.toInt() }
            cameraWidth = w
            cameraHeight = h
        }

        intent?.getStringExtra("endpoint")?.let {
            this.endpoint = it
        }

        usbMonitor = USBMonitor(this, onDeviceConnectListener).apply {
            register()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLog.e("$TAG#onDestroy")
        stopStream()
        stopPreview()
        usbMonitor?.unregister()
        usbMonitor?.destroy()
        uvcCamera?.destroy()
        usbDeviceStatus = -1
        streamStatus = -1
        MyLog.e("$TAG#onDestroy out")
    }

    private fun prepareStreamRtp() {
        stopStream()
        stopPreview()

        rtmpUSB = if (openGlView == null) {
            RtmpUSB(this, connectCheckerRtmp)
        } else {
            RtmpUSB(openGlView, connectCheckerRtmp)
        }
    }

    fun startStreamRtp(endpoint: String): Boolean {
        MyLog.e("$TAG#startStreamRtp endpoint:$endpoint, rtmpUSB.isStreaming:${rtmpUSB?.isStreaming}")
        if (rtmpUSB?.isStreaming == false) {
            this.endpoint = endpoint
//            if (rtmpUSB!!.prepareVideo(cameraWidth, cameraHeight, 30, 4000 * 1024, 0, uvcCamera) && rtmpUSB!!.prepareAudio()) {
            if (rtmpUSB!!.prepareVideo(cameraWidth, cameraHeight, 30, 1_000_000, 0, uvcCamera) && rtmpUSB!!.prepareAudio()) {
                rtmpUSB!!.startStream(uvcCamera, endpoint)
                return true
            }
        }
        return false
    }

    fun setView(view: OpenGlView) {
        openGlView = view
        rtmpUSB?.replaceView(openGlView, uvcCamera)
    }

    fun setView(context: Context) {
        openGlView = null
        rtmpUSB?.replaceView(context, uvcCamera)
    }

    fun startPreview() {
        rtmpUSB?.startPreview(uvcCamera, cameraWidth, cameraHeight)
    }

    fun stopStream(force: Boolean = false) {
        if (force) endpoint = null
        MyLog.e("$TAG rtmpUSB.isStreaming:${rtmpUSB?.isStreaming}")
        if (rtmpUSB?.isStreaming == true) rtmpUSB!!.stopStream(uvcCamera)
    }

    fun stopAnything() {
        try {
            endpoint = null
            rtmpUSB!!.stopStream(uvcCamera)
//            usbMonitor?.unregister()
            uvcCamera?.destroy()
            usbDeviceStatus = -1
            streamStatus = -1
        } catch (e: Exception) {
             MyLog.e("$TAG stopAnything exception:$e", e)
        }
    }

    fun stopPreview() {
        MyLog.e("$TAG#stopPreview isOnPreview：${rtmpUSB?.isOnPreview}")
        if (rtmpUSB?.isOnPreview == true) rtmpUSB!!.stopPreview(uvcCamera)
    }

    private val connectCheckerRtmp = object : ConnectCheckerRtmp {
        override fun onConnectionSuccessRtmp() {
            showNotification("Stream started")
            MyLog.e("$TAG RTP connection success")

            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.StreamingResult(true, 1))
            }
            streamStatus = 1
        }

        override fun onConnectionFailedRtmp(reason: String) {
            showNotification("Stream connection failed")
             MyLog.e("$TAG RTP service destroy")

            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.StreamingResult(false, 0))
            }
            streamStatus = 0
        }

        override fun onConnectionStartedRtmp(rtmpUrl: String) {
            showNotification("On connection started")
             MyLog.e("$TAG RTP On connection started")
            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.StreamingResult(true, 3))
            }
            streamStatus = 3
        }

        override fun onNewBitrateRtmp(bitrate: Long) {
//            TODO("Not yet implemented")
        }

        override fun onDisconnectRtmp() {
            showNotification("Stream stopped")

            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.StreamingResult(false, 2))
            }
            streamStatus = 2
        }

        override fun onAuthErrorRtmp() {
            showNotification("Stream auth error")
        }

        override fun onAuthSuccessRtmp() {
            showNotification("Stream auth success")
        }
    }

    private fun showNotification(text: String) {
        val notification =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setContentTitle("RTP Stream")
                .setContentText(text).build()
        notificationManager.notify(notifyId, notification)
    }

    private val onDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            MyLog.e("$TAG#onAttach: $this")
            usbDeviceStatus = 2
            usbMonitor!!.requestPermission(device)
        }

        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
            MyLog.e("$TAG#onConnect endpoint:$endpoint")
            usbDeviceStatus = 1
            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.ConnectionChanged(true))
            }
            val camera = UVCCamera()
            camera.open(ctrlBlock)
            try {
 //               val maxSupportedSize = camera.supportedSizeList.maxBy { it.width * it.height }
                camera.setPreviewSize(cameraWidth, cameraHeight, UVCCamera.FRAME_FORMAT_MJPEG)
            } catch (e: IllegalArgumentException) {
                camera.destroy()
                try {
                    camera.setPreviewSize(cameraWidth, cameraHeight, UVCCamera.DEFAULT_PREVIEW_MODE)
                } catch (e1: IllegalArgumentException) {
                    return
                }
            }
            uvcCamera = camera
            prepareStreamRtp()
            rtmpUSB!!.startPreview(uvcCamera, cameraWidth, cameraHeight)
            endpoint?.let { startStreamRtp(it) }
//            resolutions.clear()
//            uvcCamera?.let {
//                it.supportedSizeList ?.forEach { size ->
//                    MyLog.e("$TAG#supportedSizeList:${size.width}x${size.height}")
//                    resolutions.add("${size.width}x${size.height}")
//                }
//            }
//            MyLog.e("$TAG#resolutions:$resolutions")
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            MyLog.e("$TAG#onDisconnect")
            uvcCamera?.onDisconnect()
            usbDeviceStatus = 0
            coroutineScope.launch {
                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.ConnectionChanged(false))
            }
            stopStream(false)
        }

        override fun onCancel(device: UsbDevice?) {
            MyLog.e("$TAG#onCancel")
            usbDeviceStatus = 3
        }

        override fun onDettach(device: UsbDevice?) {
            MyLog.e("$TAG#onDettach")
//            coroutineScope.launch {
//                StreamEventBus.emitEvent(StreamEventBus.StreamEvent.ConnectionChanged(false))
//            }
//            usbDeviceStatus = 4
//            stopStream(false)
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): StreamService = this@StreamService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}
