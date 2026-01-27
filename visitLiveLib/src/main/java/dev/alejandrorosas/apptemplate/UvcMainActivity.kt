package dev.alejandrorosas.apptemplate

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.TypedValue
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.anmi.camera.uvcplay.MainEntryViewModel
import com.anmi.camera.uvcplay.api.VisitParams
import com.anmi.camera.uvcplay.bridge.DeviceInfoBridge
import com.anmi.camera.uvcplay.data.StreamDemuxingRepository
import dev.alejandrorosas.streamlib.StreamBridge
import com.anmi.camera.uvcplay.fragment.AnalyzeFragment
import com.anmi.camera.uvcplay.fragment.MessageNotifyFragment
import com.anmi.camera.uvcplay.fragment.HistoryFragment
import com.anmi.camera.uvcplay.ui.BaseActivity
import com.anmi.camera.uvcplay.ui.PocAlertDialog
import com.anmi.camera.uvcplay.view.FixedSizeDrawable
import com.base.MyLog
import com.base.ThreadHelper
import com.base.log.Dlog
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.MainViewModel.ViewState
import dev.alejandrorosas.streamlib.UsbDeviceManager
import com.duyansoft.usbhelperlib.UsbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UvcMainActivity : BaseActivity(R.layout.activity_main), SurfaceHolder.Callback, ServiceConnection, IServiceControlInterface {

    private val viewModel by viewModels<MainViewModel>()
    private val apiViewModel by viewModels<MainEntryViewModel>()
    private val msgFragment by lazy { MessageNotifyFragment() }
    private val analyzeFragment by lazy { AnalyzeFragment() }
    private val historyFragment by lazy { HistoryFragment() }




    @Inject
    lateinit var streamDemuxingRepository: StreamDemuxingRepository

    private val scopeJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + scopeJob)


    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            checkUsbState()
        }
    }

    companion object {
        private const val TAG = "[MainActivity]"

        private const val EXTRA_VISIT_PARAMS = "EXTRA_VISIT_PARAMS"

        @JvmStatic
        fun createIntent(context: Context, params: VisitParams): Intent {
            return Intent(context, UvcMainActivity::class.java).apply {
                putExtra(EXTRA_VISIT_PARAMS, params)
            }
        }
    }

    private fun doOnCreate() {

        initTabLayout()

        onBackPressedDispatcher.addCallback(this) {
            showSystemAlert(
                getString(R.string.text_prompt),
                getString(R.string.text_exit_app_ask),
                true,
                getString(R.string.text_cancel),
                getString(R.string.text_ok),
                onConfirm = {
                    invokeStopApi(DeviceInfoBridge.readRtmpUrl())

                    clearAllFragments()
                    // 关闭当前 Activity 以及所有父 Activity
                    finishAffinity()
                    // （API 21+）将任务从最近任务列表中移除
                    finishAndRemoveTask()

                    StreamBridge.getInstance().stopWork()

                },
                onCancel = {
                },
                tag = "exit_app"
            )
        }

        UsbDeviceManager.getInstance()
            .setUsbStateCallBack { hasConnected: Boolean, isNeedRequestPermission: Boolean ->
//                isConnected = hasConnected
//                setUsbInfo()
                UsbHelper(UsbDeviceManager.getInstance().controlPort).open()
//                UsbHelper(UsbDeviceManager.getInstance().controlPort).authDevice { batchNum: ByteArray?, deviceId: ByteArray? ->
//                    Dlog.e("authDevice 回调")
////                    if (batchNum == null || deviceId == null) {
//////                        ToastUtils.showShort("设备认证错误，请重新插入")
////                        val lastBoxImei = SpUtils.getSharedStringData(Config.KEY_BOX_IMEI)
////                        val lastBoxBatch = SpUtils.getSharedStringData(Config.KEY_BOX_BATCH)
////                        if (!TextUtils.isEmpty(lastBoxImei) && !TextUtils.isEmpty(lastBoxBatch)) {
////                            boxId = lastBoxImei;
////                            batchId = lastBoxBatch;
////                            LogUtils.e(String.format("last 批次号:%s,设备号:%s", batchId, boxId))
////                            UsbHelper(UsbDeviceManager.getInstance().micPort).auth(boxId)
////                            bindBox()
////                        }
////                    } else {
////                        boxId = StringUtils.printBytesAsBinary(deviceId)
////                        batchId = StringUtils.printBytesAsBinary(batchNum)
////                        LogUtils.e(String.format("批次号:%s,设备号:%s", batchId, boxId))
////                        bindBox()
////                    }
//                }
            }

        mHandler.removeMessages(0)
        mHandler.sendEmptyMessageDelayed(0, 3_000)

//        StreamService.openGlView = findViewById(R.id.openglview)
//        startService(getServiceIntent(null))
//
//        viewModel.serviceLiveEvent.observe(this) { mService?.let(it) }
//        viewModel.getViewState().observe(this) { render(it) }
//
//        findViewById<View>(R.id.btn_safe_exit).setOnClickListener {
////            startActivity(Intent(this, SettingsActivity::class.java))
//            viewModel.onSafeEjectButtonClick()
//            wsManager?.stopConnect()
//        }
//        findViewById<OpenGlView>(R.id.openglview).holder.addCallback(this)
//        findViewById<Button>(R.id.start_stop_stream).setOnClickListener { viewModel.onStreamControlButtonClick() }
//        findViewById<Button>(R.id.btn_setting).setOnClickListener { viewModel.onSettingButtonClick(this, this) }
    }



    private fun checkUsbState() {
        if (UsbDeviceManager.getInstance().micPort == null) {
            UsbDeviceManager.getInstance().initDevice(this)
        } else {
            Dlog.e("#checkUsbState 麦克风端口未清理");
        }
    }

    private fun clearAllFragments() {

        // 先把 FragmentManager 的回退栈全部 pop 掉 同步执行
        supportFragmentManager.popBackStackImmediate(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        // 2. 然后再把所有还残留的 Fragment remove 掉 同步执行
        supportFragmentManager.fragments.forEach { frag ->
            supportFragmentManager.beginTransaction()
                .remove(frag)
                .commitNowAllowingStateLoss()
        }
    }
    @SuppressLint("CommitTransaction")
    private fun initTabLayout() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, msgFragment, "MSG")
            add(R.id.fragment_container, analyzeFragment, "ANALYZE")
            add(R.id.fragment_container, historyFragment, "HISTORY")
            hide(analyzeFragment)
            hide(historyFragment)
            commit()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_message -> switchTo(msgFragment)
                R.id.nav_case    -> switchTo(analyzeFragment)
                R.id.nav_history    -> {
                    switchTo(historyFragment)
                }
            }
            true
        }

        applyFixedIconsToBottomNav(bottomNavigationView, 25)

        ThreadHelper.getInstance().postToUIThread({
            val menu = bottomNavigationView.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                val icon = item.icon
                icon?.let {
                    Dlog.e("ICON_CHECK " +"item=${item.title} intrinsic=${it.intrinsicWidth}x${it.intrinsicHeight} bounds=${it.bounds}")
                }
            }
        }, 3000L)
    }

    // 在你的 Activity onCreate() 或初始化 bottom nav 后调用
    private fun applyFixedIconsToBottomNav(bottomNav: BottomNavigationView, iconDp: Int = 24) {
        val sizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDp.toFloat(), bottomNav.resources.displayMetrics).toInt()

        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val original = item.icon ?: continue

            // 包装并固定尺寸（保留状态）
            val fixed = FixedSizeDrawable(original, sizePx, sizePx)
            // 明确设定 bounds，防止首次绘制时为 0
            fixed.setBounds(0, 0, sizePx, sizePx)

            // 把 fixed 设置回去
            item.icon = fixed

            // 将当前 menu item 的 checked state 传给 drawable（保证第一次显示状态正确）
            val state = if (item.isChecked) intArrayOf(android.R.attr.state_checked) else intArrayOf(-android.R.attr.state_checked)
            fixed.state = state
        }

        // 强制 BottomNavigationView 刷新
        bottomNav.invalidate()
        // 若需要可让 BottomNavigationView 重新应用选中项（触发内部 state 更新）
        bottomNav.selectedItemId = bottomNav.selectedItemId
    }


    @SuppressLint("CommitTransaction")
    private fun switchTo(target: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            // 隐藏当前所有，再显示目标
            listOf(msgFragment, analyzeFragment, historyFragment).forEach { frag ->
                if (frag == target) show(frag) else hide(frag)
            }
            commit()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(this, arrayOf(RECORD_AUDIO, CAMERA), 1)
        requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 2)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (TextUtils.isEmpty(sharedPreferences.getString("endpoint", null))){
//            sharedPreferences.edit().putString("endpoint", "rtmp://117.74.66.189:1935/live/stream5").apply()
            sharedPreferences.edit().putString("endpoint", "rtmp://122.115.57.199:1935/live/stream5").apply()
        }
        viewModel.command.observe(this) { cmd ->
            if(cmd == 1000){
                showSystemAlert(
                    getString(R.string.text_prompt),
                    getString(R.string.text_exit_app_lang),
                    true,
                    getString(R.string.text_cancel),
                    getString(R.string.text_ok),
                    onConfirm = {
                        invokeStopApi(DeviceInfoBridge.readRtmpUrl())

                        clearAllFragments()
                        // 关闭当前 Activity 以及所有父 Activity
                        finishAffinity()
                        // （API 21+）将任务从最近任务列表中移除
                        finishAndRemoveTask()

                        StreamBridge.getInstance().stopWork()

                    },
                    onCancel = {
                    },
                    tag = "lang_exit_app"
                )
            }
        }
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

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MyLog.e(TAG + "onRequestPermissionsResult, requestCode:$requestCode, permissions:${permissions.contentToString()}, grantResults:${grantResults.contentToString()}")
        if (requestCode == 1) {
            if (grantResults.isNotEmpty()) {
                grantResults.forEach {
                    if (it != 0) {
                        Toast.makeText(this, R.string.toast_permission_not_granted, Toast.LENGTH_LONG).show()
                        return
                    }
                }
                //权限被授予
                doOnCreate()
            } else {
                //权限被拒绝
                Toast.makeText(this, R.string.toast_permission_deny, Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun render(viewState: ViewState) {
        findViewById<Button>(R.id.start_stop_stream).setText(viewState.streamButtonText)
    }

//    private fun getServiceIntent(endpoint: String?): Intent {
////        val resolution = viewModel.getResolution()
////        Log.e(TAG, "#getServiceIntent resolution:$resolution")
////        return Intent(this, StreamService::class.java).putExtra("resolution", resolution).putExtra("endpoint", endpoint).also {
////            bindService(it, this, Context.BIND_AUTO_CREATE)
////        }
//    }

    private fun stopService() {
//        stopService(Intent(this, StreamService::class.java))
//        mService = null
//        unbindService(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
//        MyLog.e("surfaceChanged")
//        mService?.let {
//            it.setView(findViewById<OpenGlView>(R.id.openglview))
//            it.startPreview()
//        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
//        MyLog.e("surfaceDestroyed")
//        mService?.let {
//            it.setView(applicationContext)
//            it.stopPreview()
//        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
//        MyLog.e("surfaceCreated")
    }

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
//        mService = (service as StreamService.LocalBinder).getService()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
//        mService = null
    }

    override fun onStop() {
        super.onStop()
//        unbindService(this)
    }

    override fun onStart() {
        super.onStart()
//        getServiceIntent(null)
    }

    override fun onDestroy() {
//        try {
//            if (mService?.isStreaming == false) stopService()
//        } catch (e: Exception) {
//            Log.e(TAG, "onDestroy exception:$e", e)
//        }
        super.onDestroy()
        scope.cancel()
    }

    override fun stop(context: Context?) {
//        stopService()



//        mService?.let {
//            it.setView(applicationContext)
//            it.stopPreview()
//        }
//        mService?.stopPreview()
//        var openGlView: OpenGlView? = findViewById(R.id.openglview)
    }

    override fun start(context: Context?, endpoint: String) {
//        StreamService.openGlView = findViewById(R.id.openglview)
//        startService(getServiceIntent(endpoint))
    }



    // 封装通用提示框扩展函数
    fun showSystemAlert(
        title: String,
        content: String,
        withCancel: Boolean,
        cancelText: String = getString(R.string.text_cancel),
        confirmText: String = getString(R.string.text_ok),
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {},
        tag: String
    ) {
        PocAlertDialog.newInstance(title, content, withCancel, cancelText, confirmText, onConfirm, onCancel).also {
            it.show(supportFragmentManager, tag)
        }
    }

    private fun invokeStopApi(streamId: String) {
        MyLog.d(TAG + "invokeStopApi in")
        scope.launch {
            try {
                val resultCode = streamDemuxingRepository.stop(streamId)
                MyLog.d(TAG + "invokeStopApi resultCode:$resultCode")
            } catch (t: Throwable) {
                MyLog.e("${DeviceInfoBridge.TAG} invokeStopApi error: $t")
            }
        }
    }

}
