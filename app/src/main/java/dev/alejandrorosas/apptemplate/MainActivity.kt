package dev.alejandrorosas.apptemplate

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.MyLog
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pedro.rtplibrary.view.OpenGlView
import com.rabtman.wsmanager.WsManager
import com.rabtman.wsmanager.listener.WsStatusListener
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.MainViewModel.ViewState
import dev.alejandrorosas.streamlib.StreamService
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main), SurfaceHolder.Callback, ServiceConnection, IServiceControlInterface {

//    private var wsUrl: String  = "wss://dyai.duyansoft.com/algorithm/visit-api/v1/visit/ws/%s"
    private var wsUrl: String  = "wss://test-ai.duyansoft.com/algorithm/visit-api/v1/visit/ws/%s"
    private var wsManager: WsManager? = null
    private val viewModel by viewModels<MainViewModel>()
    private var mService: StreamService? = null
    private var messageRv: RecyclerView? = null
    private var adapter :NotifyMessageAdapter? = null
    private var voicePlayer :VoicePlayer? = null

    companion object {
        private const val TAG = "[MainActivity]"
    }

    private val wsStatusListener: WsStatusListener = object : WsStatusListener() {
        override fun onOpen(response: Response?) {
            MyLog.e("$TAG#WsManager-----onOpen")

        }

        override fun onMessage(text: String?) {
            try {
                MyLog.e("$TAG#WsManager-----onMessage:$text")
                val parseMessage = parseMessage(text)
                val notifyMessage = parseMessage?.copy(id = UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
                MyLog.e("$TAG#WsManager-----notifyMessage:$notifyMessage")
                if (notifyMessage?.success!!){
                    addNewMessage(notifyMessage)
                    onPlayVoice(notifyMessage!!)
                }
            } catch (e: Throwable) {
                MyLog.e("$TAG#WsManager-----onMessage exception:$e", e)
            }
        }

        override fun onMessage(bytes: ByteString?) {
            MyLog.e("$TAG#WsManager-----onMessage")
        }

        override fun onReconnect() {
            MyLog.e("$TAG#WsManager-----onReconnect")
        }

        override fun onClosing(code: Int, reason: String?) {
            MyLog.e("$TAG#WsManager-----onClosing")
        }

        override fun onClosed(code: Int, reason: String?) {
            MyLog.e("$TAG#WsManager-----onClosed")
        }

        override fun onFailure(t: Throwable?, response: Response?) {
            MyLog.e("$TAG#WsManager-----onFailure")
        }
    }

    fun onPlayVoice(notifyMessage: NotifyMessage) {
        val resId = when (notifyMessage.data.status) {
            "FACE_RECOGNITION_SUCCESS" -> R.raw.face_recognition_success
            "FACE_RECOGNITION_FAILURE" -> R.raw.face_recognition_failure
            "ENVIRONMENT_RECOGNITION_OLD" -> R.raw.environment_recognition_old
            "AUDIO_EMOTION_ANALYSIS_EXCITE" -> R.raw.audio_emotion_analysis_excite
            "VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL" -> R.raw.video_emotion_behavior_analysis_abnormal
            else                        -> 0
        }

        if (resId > 0){
            voicePlayer?.play(resId)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(this, arrayOf(RECORD_AUDIO, CAMERA), 1)
        requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 2)
//
//        val timer = Timer()
//        timer.scheduleAtFixedRate(
//            object : TimerTask() {
//                override fun run() {
//                    Handler(Looper.getMainLooper()).post {
//                        wsStatusListener.onMessage("{\n" +
//                            "    \"type\": \"ENVIRONMENT_RECOGNITION\",\n" +
//                            "    \"success\": true,\n" +
//                            "    \"data\": {\n" +
//                            "        \"status\": \"ENVIRONMENT_RECOGNITION_OLD\",  // 人脸识别成功标识\n" +
//                            "        \"message\": \"现场环境分析用户资产较低，催收难度大，请谨慎处理\"\n" +
//                            "    }\n" +
//                            "}\n")
//                    }
//                }
//            },
//            10*1000,    // 初始延迟
//            30*1000 // 间隔时间
//        )

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("endpoint", "rtmp://dyai.duyansoft.com/live/stream5").apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MyLog.e(TAG + "onRequestPermissionsResult, requestCode:$requestCode, permissions:${permissions.contentToString()}, grantResults:${grantResults.contentToString()}")
        if (requestCode == 1) {
            if (grantResults.isNotEmpty()) {
                grantResults.forEach {
                    if (it != 0) {
                        Toast.makeText(this, "权限获取失败，无法使用", Toast.LENGTH_LONG).show()
                        return
                    }
                }
                //权限被授予
                StreamService.openGlView = findViewById(R.id.openglview)
                startService(getServiceIntent(null))

                viewModel.serviceLiveEvent.observe(this) { mService?.let(it) }
                viewModel.getViewState().observe(this) { render(it) }

                findViewById<View>(R.id.btn_safe_exit).setOnClickListener {
//            startActivity(Intent(this, SettingsActivity::class.java))
                    viewModel.onSafeEjectButtonClick()
                    wsManager?.stopConnect()
                }
                findViewById<OpenGlView>(R.id.openglview).holder.addCallback(this)
                findViewById<Button>(R.id.start_stop_stream).setOnClickListener { viewModel.onStreamControlButtonClick() }
                findViewById<Button>(R.id.btn_setting).setOnClickListener { viewModel.onSettingButtonClick(this, this) }
                initMessageList()
                initWebSocket()
                viewModel.setStreamCallback(object : IStreamStateCallback {
                    override fun onStartStream() {
                        MyLog.e("推流已开始，打开 WebSocket 连接")
                        wsManager?.startConnect()

                    }
                    override fun onStopStream() {
                        MyLog.e("推流已结束，关闭 WebSocket 连接")
                        wsManager?.stopConnect()
                    }
                })
                voicePlayer = VoicePlayer(this)
            } else {
                //权限被拒绝
                Toast.makeText(this, "权限为空，无法使用", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initWebSocket() {
        if (TextUtils.isEmpty(wsUrl)){
            return
        }
        wsUrl = String.format(wsUrl, System.currentTimeMillis())
        MyLog.e("[initWebSocket]wsUrl：$wsUrl")
        if (wsManager != null) {
            wsManager!!.stopConnect();
            wsManager = null
        }
// 1. 创建日志拦截器
        // 1. 创建日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor { message -> MyLog.e("[OkHttp]$message") }
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient = OkHttpClient().newBuilder()
            .pingInterval(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()

        wsManager = WsManager.Builder(baseContext)
            .client(okHttpClient)
            .needReconnect(true)
            .wsUrl(wsUrl)
            .build()
        wsManager?.let { manager ->
            manager.setWsStatusListener(wsStatusListener)
//            manager.startConnect()
        }
    }

    private fun parseMessage(text: String?): NotifyMessage? {
        return try {
            Gson().fromJson(text, NotifyMessage::class.java)
        } catch (e: JsonSyntaxException) {
            MyLog.e("$TAG#parseMessage exception:${e.message}", e)
            null
        }
    }

    private fun addNewMessage(message: NotifyMessage?) {
        if (message == null) {
            return
        }
        val newList = mutableListOf<NotifyMessage>().apply {
            add(message) // 添加新消息到列表顶部
            adapter?.let { addAll(it.currentList) }
        }
        adapter?.submitList(newList)
        messageRv?.smoothScrollToPosition(0)
    }

    private fun initMessageList() {
        messageRv = findViewById(R.id.message_list)
        adapter = NotifyMessageAdapter()
        messageRv.let {
            it?.adapter = adapter
            it?.layoutManager = LinearLayoutManager(this).apply {
                reverseLayout = false // 设置反转布局
                stackFromEnd = false // 从顶部开始显示
            }
            it?.itemAnimator = DefaultItemAnimator().apply {
                // 禁用不必要的动画
                supportsChangeAnimations = false
            }
        }
    }

    private fun render(viewState: ViewState) {
        findViewById<Button>(R.id.start_stop_stream).setText(viewState.streamButtonText)
    }

    private fun getServiceIntent(endpoint: String?): Intent {
        val resolution = viewModel.getResolution()
        Log.e(TAG, "#getServiceIntent resolution:$resolution")
        return Intent(this, StreamService::class.java).putExtra("resolution", resolution).putExtra("endpoint", endpoint).also {
            bindService(it, this, Context.BIND_AUTO_CREATE)
        }
    }

    private fun stopService() {
        stopService(Intent(this, StreamService::class.java))
        mService = null
        unbindService(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        MyLog.e("surfaceChanged")
        mService?.let {
            it.setView(findViewById<OpenGlView>(R.id.openglview))
            it.startPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        MyLog.e("surfaceDestroyed")
        mService?.let {
            it.setView(applicationContext)
            it.stopPreview()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        MyLog.e("surfaceCreated")
    }

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        mService = (service as StreamService.LocalBinder).getService()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
        mService = null
    }

    override fun onStop() {
        super.onStop()
        unbindService(this)
    }

    override fun onStart() {
        super.onStart()
        getServiceIntent(null)
    }

    override fun onDestroy() {
        try {
            if (mService?.isStreaming == false) stopService()
            voicePlayer?.release()
            voicePlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy exception:$e", e)
        }
        super.onDestroy()
    }

    override fun stop(context: Context?) {
//        mService?.let {
//            it.setView(applicationContext)
//            it.stopPreview()
//        }
//        mService?.stopPreview()
        stopService()
//        var openGlView: OpenGlView? = findViewById(R.id.openglview)
    }

    override fun start(context: Context?, endpoint: String) {
        StreamService.openGlView = findViewById(R.id.openglview)
        startService(getServiceIntent(endpoint))
    }
}
