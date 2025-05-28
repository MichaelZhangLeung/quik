package dev.alejandrorosas.apptemplate

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.anmi.camera.uvcplay.fragment.AnalyzeFragment
import com.anmi.camera.uvcplay.fragment.MessageNotifyFragment
import com.anmi.camera.uvcplay.ui.PocAlertDialog
import com.base.MyLog
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.MainViewModel.ViewState


@AndroidEntryPoint
class UvcMainActivity : AppCompatActivity(R.layout.activity_main), SurfaceHolder.Callback, ServiceConnection, IServiceControlInterface {

    private val viewModel by viewModels<MainViewModel>()
    private val msgFragment by lazy { MessageNotifyFragment() }
    private val analyzeFragment by lazy { AnalyzeFragment() }

    companion object {
        private const val TAG = "[MainActivity]"
    }

    private fun doOnCreate() {

        initTabLayout()

        onBackPressedDispatcher.addCallback(this) {

            showSystemAlert(
                "提示",
                "是否确定退出外访应用？",
                true,
                "取消",
                "确定",
                onConfirm = {
                    clearAllFragments()
                    // 关闭当前 Activity 以及所有父 Activity
                    finishAffinity()
                    // （API 21+）将任务从最近任务列表中移除
                    finishAndRemoveTask()

                },
                onCancel = {
                },
                tag = "exit_app"
            )
        }
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

    private fun clearAllFragments() {
        // 1. 先把 FragmentManager 的回退栈全部 pop 掉 同步执行
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
        // 第一次添加两个 Fragment，并仅显示消息页
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, msgFragment, "MSG")
            add(R.id.fragment_container, analyzeFragment, "ANALYZE")
            hide(analyzeFragment)
            commit()
        }

        findViewById<BottomNavigationView>(R.id.bottom_nav).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_message -> switchTo(msgFragment)
                R.id.nav_case    -> switchTo(analyzeFragment)
            }
            true
        }
    }

    @SuppressLint("CommitTransaction")
    private fun switchTo(target: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            // 隐藏当前所有，再显示目标
            listOf(msgFragment, analyzeFragment).forEach { frag ->
                if (frag == target) show(frag) else hide(frag)
            }
            commit()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(this, arrayOf(RECORD_AUDIO, CAMERA), 1)
        requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 2)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (TextUtils.isEmpty(sharedPreferences.getString("endpoint", null))){
            sharedPreferences.edit().putString("endpoint", "rtmp://117.74.66.189:1935/live/stream5").apply()
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
                        Toast.makeText(this, "权限获取失败，无法使用", Toast.LENGTH_LONG).show()
                        return
                    }
                }
                //权限被授予
                doOnCreate()
            } else {
                //权限被拒绝
                Toast.makeText(this, "权限为空，无法使用", Toast.LENGTH_LONG).show()
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
        super.onDestroy();
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
        cancelText: String = "取消",
        confirmText: String = "确定",
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {},
        tag: String
    ) {
        PocAlertDialog.newInstance(title, content, withCancel, cancelText, confirmText, onConfirm, onCancel).also {
            it.show(supportFragmentManager, tag)
        }
    }


}
