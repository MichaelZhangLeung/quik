package com.anmi.camera.uvcplay.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anmi.camera.uvcplay.CasesChooseActivity
import com.anmi.camera.uvcplay.MainEntryViewModel
import com.anmi.camera.uvcplay.locale.LocaleHelper
import com.anmi.camera.uvcplay.model.CaseModel
import com.anmi.camera.uvcplay.tts.TtsStreamClient
import com.anmi.camera.uvcplay.ui.PocAlertDialog
import com.anmi.camera.uvcplay.utils.Utils
import com.anmi.camera.uvcplay.utils.Utils.setDebounceClickListener
import com.anmi.camera.uvcplay.utils.Utils.wrapNotifyStatus
import com.base.MyLog
import com.base.ThreadHelper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pedro.rtplibrary.view.OpenGlView
import com.rabtman.wsmanager.WsManager
import com.rabtman.wsmanager.listener.WsStatusListener
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.Data
import dev.alejandrorosas.apptemplate.IServiceControlInterface
import dev.alejandrorosas.apptemplate.IStreamStateCallback
import dev.alejandrorosas.apptemplate.MainViewModel
import dev.alejandrorosas.apptemplate.NotifyMessage
import dev.alejandrorosas.apptemplate.NotifyMessageAdapter
import dev.alejandrorosas.apptemplate.R
import dev.alejandrorosas.apptemplate.SettingsActivity
import dev.alejandrorosas.apptemplate.VoicePlayer
import dev.alejandrorosas.streamlib.SerialEarPlayer
import dev.alejandrorosas.streamlib.StreamBridge
import dev.alejandrorosas.streamlib.StreamEventBus
import dev.alejandrorosas.streamlib.StreamService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MessageNotifyFragment : Fragment(), SurfaceHolder.Callback, ServiceConnection, IServiceControlInterface {

    companion object {
        private const val TAG = "[MessageNotifyFragment]"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private val apiViewModel: MainEntryViewModel by activityViewModels()

    private var mLayoutInflaterView: View? = null
    private var openGlView: OpenGlView? = null
    private var rlUsbDisConnectWarning: RelativeLayout? = null
    private var pushStreamBtn: Button? = null
    private var flEmptyCaseChoose: FrameLayout? = null
    private var messageRv: RecyclerView? = null
    private var messageFl: FrameLayout? = null
    private var messageEmptyRl: RelativeLayout? = null
    private var caseRunningLayout: LinearLayout? = null
    private var adapter :NotifyMessageAdapter? = null
//    private var wsUrl: String  = "wss://172.21.66.2:15658/v1/visit/ws/%s"
    private var wsUrl: String  = "wss://myvap.duyansoft.com/algorithm/visit-api/v1/visit/ws/%s"
    //    private var wsUrl: String  = "wss://test-ai.duyansoft.com/algorithm/visit-api/v1/visit/ws/%s"
//    private var wsUrlTemplate: String  = "$wsUrl/%s"
    private var wsManager: WsManager? = null
    private var voicePlayer : VoicePlayer? = null
    private var mService: StreamService? = null
    private var chooseCaseData: CaseModel? = null

    private var deviceConnected:Boolean = false
    private var streamSuccess:Boolean = false

    // 用于定时任务的 Job
    private var latencyJob: Job? = null
    private val rttScheduler = Executors.newSingleThreadScheduledExecutor()
    private var rttFuture: ScheduledFuture<*>? = null

    private var locale: String? = null


    @SuppressLint("NewApi")
    private val openForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // 在这里处理返回值
            val value = data?.getSerializableExtra("case", CaseModel::class.java)
            val visitId = data?.getStringExtra("visit_id")
            MyLog.e("case select back:$value, ret_visitId:$visitId, viewModel.getVisitId():${viewModel.getVisitId()}")
            if (!TextUtils.equals(visitId, viewModel.getVisitId())){
                viewModel.setVisitId(visitId)
            }
            value?.let {
                chooseCaseData = it
                viewModel.setCaseModel(chooseCaseData)
                switchCaseRunningLayout(true)
                onCaseChoose()
            }
        }
    }

    @SuppressLint("NewApi")
    private val openSettingsForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->


        mLayoutInflaterView?.let {
            it.findViewById<TextView>(R.id.tv_resolution).text = viewModel.getResolution()
        }

        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val settingResult = data?.getIntExtra("setting_result", 0)
            if (settingResult == 1000){
                viewModel.sendCommand(settingResult)
            }
//            val value = data?.getIntExtra("changed", 0)
//            Toast.makeText(requireContext(), "Settings Got: $value", Toast.LENGTH_SHORT).show()
//            if (value != 0){
//                onStatusBarChange()
//            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onStatusBarChange() {
        when (deviceConnected) {
            true -> {
                mLayoutInflaterView?.findViewById<TextView>(R.id.tv_resolution)?.text = viewModel.getResolution()
                mLayoutInflaterView?.findViewById<TextView>(R.id.tv_fps)?.text = "${viewModel.getFps()}fps"
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.tv_stream_info)?.visibility = View.VISIBLE

                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_stream)?.visibility  = View.VISIBLE
                val tvStreamBadge = mLayoutInflaterView?.findViewById<TextView>(R.id.tv_stream_badge)
                tvStreamBadge?.updateLayoutParams<FrameLayout.LayoutParams> {
                    if (locale == "en"){
                        width  = context?.dpToPx(40)!!
                    } else{
                        width  = context?.dpToPx(22)!!
                    }

                    height = context?.dpToPx(13)!!
                }
                tvStreamBadge?.apply {
                    if (locale == "en"){
                        setBackgroundResource(R.mipmap.icon_stream_ready_en)
                    } else{
                        setBackgroundResource(R.mipmap.icon_stream_ready)
                    }
                }

                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_inject_device)?.visibility  = View.VISIBLE
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_stream)?.setDebounceClickListener(2000L) {
                    viewModel.onStreamControlButtonClick()
                }
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_inject_device)?.setDebounceClickListener(2000L) {
                    viewModel.onSafeEjectButtonClick()
                    wsManager?.stopConnect()
//                    StreamBridge.getInstance().stopWork()
                }
            }
            false -> {
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.tv_stream_info)?.visibility = View.GONE
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_stream)?.visibility  = View.GONE
                mLayoutInflaterView?.findViewById<FrameLayout>(R.id.fl_inject_device)?.visibility  = View.GONE
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_message_notify, container, false).also { view ->
        mLayoutInflaterView = view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        locale = LocaleHelper.getSavedLanguage(requireContext())

        initPreview(view)
        initEmptyCaseChoose(view)
        initMessageList(view)
        initWebSocket()
        switchCaseRunningLayout(false)
        voicePlayer = context?.let { VoicePlayer(it) }
        viewModel.setStreamCallback(
            object : IStreamStateCallback {
                override fun onStartStream() {
                    MyLog.e("${TAG}推流已开始，打开 WebSocket 连接")

//                    if (viewModel.getVisitId() == null){// 结束外访时已置空，重新生成外访id
//                        initWebSocket()
//                    }

                    if (!checkVisitIdForWsUrl()){// 检查url中与缓存的visitid是否一致
                        initWebSocket()
                    }

                    wsManager?.startConnect()

                    MyLog.e("${TAG}案件状态：${viewModel.caseState.value?.code}")
                    if (viewModel.caseState.value?.code == 2) {//案件选择后未开启推流
                        //通知加载web url
                        viewModel.notifyCaseEventStatus(true, 1)
                    }
                }

                override fun onStopStream() {
                    MyLog.e("${TAG}推流已结束，关闭 WebSocket 连接")
                    wsManager?.stopConnect()
                }

                override fun onDeviceConnected() {
                    TODO("Not yet implemented")
                }

                override fun onDeviceDisconnected() {
                    TODO("Not yet implemented")
                }

                override fun onRtmpStreamConnected() {
                    TODO("Not yet implemented")
                }

                override fun onRtmpStreamFailed() {
                    TODO("Not yet implemented")
                }

                override fun onSafeInject() {
                }
            },
        )
        collectStreamEvents()
        onStatusBarChange()
    }


    private var eventJob: Job? = null

    private fun collectStreamEvents() {
        eventJob = lifecycleScope.launch {
            StreamEventBus.events
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is StreamEventBus.StreamEvent.ConnectionChanged -> handleConnection(event.isConnected)
                        is StreamEventBus.StreamEvent.StreamingResult -> handleStreamResult(event)
                    }
                }
        }
    }



    private fun startLatencyLoop() {
        MyLog.e("#startLatencyLoop in")
        rttFuture?.cancel(true)
        val endpoint = viewModel.getEndpoint()
        endpoint?.let { it ->
            val port = Uri.parse(it).port
            val serverPort = if (port > 0) port else 1935

            Uri.parse(it).host?.let { serverHost ->
                rttFuture = rttScheduler.scheduleAtFixedRate({
                    val rtt = measureRttBlocking(serverHost, serverPort, 500)
                    requireActivity().runOnUiThread {
                        mLayoutInflaterView?.findViewById<TextView>(R.id.tv_latency)?.text = when {
                            rtt == -1L -> "--"
                            else -> "${rtt}ms"
                        }
                    }
                }, 0, 1, TimeUnit.SECONDS)
            }
        }
    }

//    /**
//     * 测量一次 TCP 握手的 RTT（毫秒）；
//     * 如果失败，返回 -1。
//     */
//    private fun measureRtt(host: String, port: Int, timeoutMs: Int): Long {
//        val start = SystemClock.elapsedRealtime()
//        return try {
//            Socket().use { socket ->
//                socket.soTimeout = timeoutMs
//                socket.connect(InetSocketAddress(host, port), timeoutMs)
//            }
//            SystemClock.elapsedRealtime() - start
//        } catch (e: Exception) {
//            -1
//        }
//    }

    private fun measureRttBlocking(host: String, port: Int, timeoutMs: Int): Long {
        return try {
            val start = SystemClock.elapsedRealtime()
            Socket().use { sock ->
                sock.soTimeout = timeoutMs
                sock.connect(InetSocketAddress(host, port), timeoutMs)
            }
            SystemClock.elapsedRealtime() - start
        } catch (e: Throwable) {
            MyLog.e("${TAG}measureRttBlocking exception:$e", e)
            -1L
        }
    }

    private fun handleStreamResult(event: StreamEventBus.StreamEvent.StreamingResult) {

        val ivStream = mLayoutInflaterView?.findViewById<ImageView>(R.id.iv_stream)
        val tvStreamBadge = mLayoutInflaterView?.findViewById<TextView>(R.id.tv_stream_badge)
        tvStreamBadge?.updateLayoutParams<FrameLayout.LayoutParams> {
            width = if (locale == "en"){
                context?.dpToPx(if (event.errorCode == 1) 49 else 40)!!
            } else{
                context?.dpToPx(if (event.errorCode == 1) 35 else 22)!!
            }

            height = context?.dpToPx(13)!!
        }
        when (event.success) {
            true -> {
                //此处回调两次，code=3 开始连接->code=1 推流成功
                MyLog.e("${TAG}推流成功:${event.errorCode}")
                if (event.errorCode == 1){
                    Utils.toast(getString(R.string.toast_streaming_ok))
                }
                streamSuccess = true
                tvStreamBadge?.apply {
                    if (locale == "en"){
                        setBackgroundResource(R.mipmap.icon_streaming_en)
                    } else{
                        setBackgroundResource(R.mipmap.icon_streaming)
                    }
                }
                ivStream?.setImageResource(R.drawable.icon_streaming_main)

                mLayoutInflaterView?.let { it ->
                    it.findViewById<FrameLayout>(R.id.fl_stream_warning).visibility = View.GONE
                    val tvRtt = it.findViewById<TextView>(R.id.tv_latency)
                    tvRtt.visibility  = VISIBLE
                }
                // 开始实时测延迟
//                if(event.errorCode == 1){
//                    startLatencyLoop()
//                }
            }
            false -> {
                MyLog.e("${TAG}推流断开：${event.errorCode}")
//                Utils.toast("推流断开：${event.errorCode}")
                Utils.toast(getString(R.string.toast_stream_disconnect))
                streamSuccess = false
                tvStreamBadge?.apply {
                    if (locale == "en"){
                        setBackgroundResource(R.mipmap.icon_stream_ready_en)
                    } else{
                        setBackgroundResource(R.mipmap.icon_stream_ready)
                    }
                }
                ivStream?.setImageResource(R.drawable.icon_stream_ready_main)

                mLayoutInflaterView?.let { it ->
                    it.findViewById<FrameLayout>(R.id.fl_stream_warning).visibility = View.VISIBLE

                    val tvRtt = it.findViewById<TextView>(R.id.tv_latency)
                    tvRtt.visibility  = INVISIBLE
                }
                // 停止测延迟
//                stopLatencyLoop()

                try {
                    wsManager?.stopConnect()
                } catch (e: Throwable) {
                    MyLog.e("${TAG}wsManager stopConnect:$e", e)
                }
            }
        }
    }

    private fun stopLatencyLoop() {
        MyLog.e("#stopLatencyLoop in")
        rttFuture?.cancel(true)
        rttFuture = null
    }

    private fun handleConnection(connected: Boolean) {
        when (connected) {
            true -> {
                MyLog.e("${TAG}设备连接成功:" + rlUsbDisConnectWarning)
                Utils.toast(getString(R.string.toast_device_connected))
                deviceConnected = true
                openGlView?.visibility = View.VISIBLE
                rlUsbDisConnectWarning?.visibility = View.GONE
            }
            false -> {
                MyLog.e("${TAG}设备断开")
                Utils.toast(getString(R.string.toast_device_disconnect))
                deviceConnected = false
//                openGlView?.visibility = View.GONE
                rlUsbDisConnectWarning?.visibility = View.VISIBLE

                showSystemAlert(
                    getString(R.string.text_sys_prompt),
                    getString(R.string.text_camera_check_failed),
                    false,
                    "",
                    getString(R.string.text_confirm),
                    onConfirm = {
                    },
                    onCancel = {
                    },
                    tag = "usb_disconnect_alert"
                )
            }
        }

        onStatusBarChange()
    }

    var ttsStreamClient:TtsStreamClient? = null

    private fun initPreview(view: View){
        openGlView = view.findViewById(R.id.openglview)
        rlUsbDisConnectWarning = view.findViewById(R.id.rl_usb_disconnect)
        StreamService.openGlView = openGlView
        activity?.startService(getServiceIntent(null))
        viewModel.serviceLiveEvent.observe(viewLifecycleOwner) { mService?.let(it) }
        viewModel.getViewState().observe(viewLifecycleOwner) { render(it) }

//        view.findViewById<View>(R.id.btn_safe_exit).setOnClickListener {
////            startActivity(Intent(this, SettingsActivity::class.java))
//            viewModel.onSafeEjectButtonClick()
//            wsManager?.stopConnect()
//        }
        openGlView?.holder?.addCallback(this)
        pushStreamBtn = view.findViewById(R.id.start_stop_stream)
        pushStreamBtn?.setOnClickListener { viewModel.onStreamControlButtonClick() }
//        view.findViewById<Button>(R.id.btn_setting).setOnClickListener { context?.let { it1 -> viewModel.onSettingButtonClick(it1, this) } }

        view.findViewById<FrameLayout>(R.id.fl_settings).setOnClickListener {
            if (StreamService.streamStatus == 1){
                Toast.makeText(requireContext(), R.string.toast_stop_streaming_when_setting, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), SettingsActivity::class.java)
            openSettingsForResult.launch(intent)

//            var data = Data("VISIT_VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL", "VISIT_VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL")
//            var notifyData = NotifyMessage("", "", true, data, 0L)
//
//            onPlayVoice(notifyData)
        }


//        view.findViewById<FrameLayout>(R.id.fl_settings).setOnClickListener {
//
//            var data = Data("VISIT_VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL", "VISIT_VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL")
//            var notifyData = NotifyMessage("", "", true, data, 0L)
//
//            onPlayVoice(notifyData)
//
////            StreamBridge.getInstance().startWork()
//
////            if (SerialEarPlayer.sSerialEarPlayer != null) {
////                Utils.toast("测试停止播放")
////                ttsStreamClient?.disconnect()
////                SerialEarPlayer.testStopPlay(requireContext())
////            } else {
////                Utils.toast("测试播放")
////                val serialEarPlayer = SerialEarPlayer.testPlay(requireContext())
////                ttsStreamClient =
////                    TtsStreamClient("wss://test-ai.duyansoft.com/automind/auto-test-platform/v1/auto/audio/visit/tts", serialEarPlayer, 512)
////                ttsStreamClient?.setListener(
////                    object : TtsStreamClient.Listener {
////                        override fun onOpen() {
////                            MyLog.e("tts ws open")
////                        }
////
////                        override fun onClose(code: Int, reason: String) {
////                            MyLog.e("tts ws close")
////                        }
////
////                        override fun onError(t: Throwable) {
////                            MyLog.e("tts error", t)
////                        }
////
////                        override fun onInfo(info: String) {
////                            MyLog.e("info: $info")
////                        }
////                    },
////                )
////                ttsStreamClient?.connect()
////
////                ThreadHelper.getInstance().postDelayed(
////                    {
////                        ttsStreamClient!!.speak("Hello! 　1.国民大会不以集会之方法行使四权，而以全体国民各在其住居地点行使选举、罢免、创制、复决之四权。他说：“因我国民情散漫，公民智识更未普及，假设各地人民得不以组织国民大会之方式，而在原地行使四权。设使有人利用此点，随时号召各地之选民实行四权，则国家基础即随时摇动，而陷于不安之状态，故此项原则为最不妥善者。")
////                    },
////                    2000L,
////                )
////            }
//
////            if (SerialEarPlayer.sSerialEarPlayer != null){
////                Utils.toast("测试停止播放")
////                SerialEarPlayer.testStopPlay(requireContext())
////            } else {
////                Utils.toast("测试播放")
////                val serialEarPlayer = SerialEarPlayer.testPlay(requireContext())
////            }
//
////            Utils.toast("测试耳机写入")
////            PcmPlayerUtils().sendStream(UsbDeviceManager.getInstance().micPort)
//        }
    }

    private fun initEmptyCaseChoose(view: View) {
        flEmptyCaseChoose = view.findViewById(R.id.fl_case_choose)
        val selectCaseBtn = view.findViewById<Button>(R.id.btn_select_case)
//        if (LocaleHelper.getSavedLanguage(requireContext()) == "en"){
//            selectCaseBtn.setTextSize()
//        }
        selectCaseBtn.setOnClickListener {

            MyLog.e("select start onClick, streamStatus:${StreamService.streamStatus}, usbDeviceStatus:${StreamService.usbDeviceStatus}")

            if (StreamService.usbDeviceStatus != 1){
                Toast.makeText(requireContext(), R.string.toast_notify_device_connect_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), CasesChooseActivity::class.java).apply {
                putExtra("visit_id", viewModel.getVisitId())
                putExtra("ws_url", wsUrl)
                putExtra("stream_success", streamSuccess)
            }
            openForResult.launch(intent)
        }
    }

    // 封装通用提示框扩展函数
    fun Fragment.showSystemAlert(
        title: String,
        content: String,
        withCancel:Boolean,
        cancelText: String = getString(R.string.text_cancel),
        confirmText: String = getString(R.string.text_ok),
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {},
        tag:String
    ) {
        PocAlertDialog.newInstance(title, content, withCancel, cancelText, confirmText, onConfirm, onCancel).also {
            it.show(childFragmentManager, tag)
        }
    }

    private fun onCaseChoose() {

        // 此时推流若开启， 加载web， 推流未开不加载web，开启推流后若已结束外访不加载，为结束外访开始加载
        if (StreamService.streamStatus != 1){
//            flEmptyCaseChoose?.let {
//                it.findViewById<TextView>(R.id.tv_empty_message).text = "摄像头未开启推流，开启推流后可进行外访作业。"
//            }

            //通知当前案件状态 - 已选择未推流
            viewModel.notifyCaseEventStatus(true, 2)

            showSystemAlert(
                getString(R.string.text_stream_prompt),
                getString(R.string.text_stream_check_failed),
                true,
                getString(R.string.text_stream_enable_delay),
                getString(R.string.text_stream_enable),
                onConfirm = {
                    initCaseChoose()
                    viewModel.onStreamControlButtonClick()
                },
                onCancel = {
                    //todo show warning text
                    mLayoutInflaterView?.let { it ->
                        it.findViewById<FrameLayout>(R.id.fl_stream_warning).visibility = View.VISIBLE
                    }
                    initCaseChoose()
                },
                tag = "push_stream_alert"
            )
//            PocAlertDialog.newInstance("推流提示", "推流未开启，请先开启推流后进行外访作业。", true, "稍后开启", "开启推流", object : PocAlertDialog.OnClickListener {})
            return
        }
        if(viewModel.getVisitId() == null){
            //推流已开启，解析推流时连接的ws url中visitId，继续使用
            val wsVisitId = wsUrl.substringAfterLast('/')
            MyLog.e("onCaseChoose set visitId from ws url：$wsVisitId")
            wsVisitId.let {
                viewModel.setVisitId(it)
            }
        }
        initCaseChoose()
        //通知加载web url
        viewModel.notifyCaseEventStatus(true, 1)

    }

    private fun initCaseChoose(){
        mLayoutInflaterView?.let {
            val targetName = chooseCaseData?.case_debtor
            val caseId = chooseCaseData?.case_id
            val caseAddress = chooseCaseData?.visit_address

            val tvDebtLabel = it.findViewById<TextView>(R.id.tv_label)
            tvDebtLabel?.apply {
                if (locale == "en"){
                    setBackgroundResource(R.mipmap.icon_debter_en)
                } else {
                    setBackgroundResource(R.mipmap.icon_debter)
                }
            }

            it.findViewById<TextView>(R.id.tv_name).text= targetName
//            it.findViewById<TextView>(R.id.tv_case_no).text= "${getString(R.string.text_predix_case_number)}：$caseId"
            it.findViewById<TextView>(R.id.tv_case_no).text= getString(R.string.text_predix_format_case,
                getString(R.string.text_predix_case_number),
                caseId)
//            it.findViewById<TextView>(R.id.tv_address).text= getString(R.string.text_predix_case_address) + "：$caseAddress"
            it.findViewById<TextView>(R.id.tv_address).text= getString(R.string.text_predix_format_case,
                getString(R.string.text_predix_case_address),
                caseAddress)

            it.findViewById<TextView>(R.id.btn_end_visit).setOnClickListener {

                showSystemAlert(
                    getString(R.string.text_prompt),
                    getString(R.string.text_ask_end_case),
                    true,
                    onConfirm = {
                        try {
                            switchCaseRunningLayout(false)
                            adapter?.clearList()
                            switchMessageListLayout(false)

                            if (StreamService.streamStatus == 1)
                                viewModel.onStreamControlButtonClick()

                            apiViewModel.endVisit(chooseCaseData, viewModel.getVisitId()!!)
//                viewModel.onSafeEjectButtonClick()
//                wsManager?.stopConnect()
//                        voicePlayer?.release()
                            chooseCaseData = null

                            //通知加载web url
                            viewModel.notifyCaseEventStatus(false, 0)

                            viewModel.setVisitId(null)
                            viewModel.setCaseModel(null)
                        } catch (e: Throwable) {
                            MyLog.e("$TAG endVisit confirm exception:$e", e)
                        }
                    },
                    onCancel = {
                    },
                    tag = "end_visit_alert"
                )
            }
        }
    }

    private fun initMessageList(view: View) {
        caseRunningLayout = view.findViewById(R.id.ll_case_running)

        messageFl = view.findViewById(R.id.fl_message_list)
        messageEmptyRl = view.findViewById(R.id.rl_message_empty)

        messageRv = view.findViewById(R.id.message_list)
        adapter = NotifyMessageAdapter()
        messageRv.let {
            it?.adapter = adapter
            it?.layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = false // 设置反转布局
                stackFromEnd = false // 从顶部开始显示
            }
            it?.itemAnimator = DefaultItemAnimator().apply {
                // 禁用不必要的动画
                supportsChangeAnimations = false
            }
        }
        switchMessageListLayout(adapter!!.itemCount > 0)
    }



    private fun initWebSocket() {
        if (TextUtils.isEmpty(wsUrl)){
            return
        }

        var visitId = viewModel.getVisitId()//1.首次为null，
        if (TextUtils.isEmpty(visitId)){
            visitId = Utils.generateVisitId()
            viewModel.setVisitId(visitId)
            MyLog.e("[initWebSocket]generateVisitId：$visitId")
        }
        val prefix = wsUrl.substringBeforeLast('/')  // "wss://myvap.duyansoft.com/algorithm/visit-api/v1/visit/ws"
        wsUrl = "$prefix/$visitId"
//        wsUrl = String.format(wsUrl, visitId)
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

        wsManager = WsManager.Builder(context)
            .client(okHttpClient)
            .needReconnect(true)
            .wsUrl(wsUrl)
            .build()
        wsManager?.let { manager ->
            manager.setWsStatusListener(wsStatusListener)
        }
    }

    private fun switchCaseRunningLayout(isRunning: Boolean) {
        if (isRunning) {
            caseRunningLayout?.visibility = View.VISIBLE
            flEmptyCaseChoose?.visibility = View.GONE
        } else {
            caseRunningLayout?.visibility = View.GONE
            flEmptyCaseChoose?.visibility = View.VISIBLE
        }
    }

    private fun switchMessageListLayout(hasData: Boolean) {
        if (hasData) {
            messageFl?.visibility = View.VISIBLE
            messageEmptyRl?.visibility = View.GONE
        } else {
            messageFl?.visibility = View.GONE
            messageEmptyRl?.visibility = View.VISIBLE
        }
    }


    private fun parseMessage(text: String?): NotifyMessage? {
        return try {
            Gson().fromJson(text, NotifyMessage::class.java)
        } catch (e: JsonSyntaxException) {
            MyLog.e("${TAG}#parseMessage exception:${e.message}", e)
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
        if (messageFl?.visibility == GONE) {
            switchMessageListLayout(true)
        }
    }


    fun onPlayVoice(notifyMessage: NotifyMessage) {
        val resId = when (notifyMessage.data.status) {
            wrapNotifyStatus("FACE_RECOGNITION_SUCCESS") -> R.raw.face_recognition_success
            wrapNotifyStatus("FACE_RECOGNITION_FAILURE") -> R.raw.face_recognition_failure
            wrapNotifyStatus("ENVIRONMENT_RECOGNITION_OLD") -> R.raw.environment_recognition_old
            wrapNotifyStatus("AUDIO_EMOTION_ANALYSIS_EXCITE") -> R.raw.audio_emotion_analysis_excite
            wrapNotifyStatus("VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL") -> R.raw.video_emotion_behavior_analysis_abnormal
            else                        -> 0
        }
        MyLog.e("${TAG}#onPlayVoice-----$resId")
        if (resId > 0){
            voicePlayer?.play(resId)
        }
    }


    private val wsStatusListener: WsStatusListener = object : WsStatusListener() {
        override fun onOpen(response: Response?) {
            MyLog.e("${TAG}#WsManager-----onOpen")

        }

        override fun onMessage(text: String?) {
            try {
                MyLog.e("$TAG#WsManager-----onMessage:$text")
                val parseMessage = parseMessage(text)
                val notifyMessage = parseMessage?.copy(id = UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
                MyLog.e("${TAG}#WsManager-----notifyMessage:$notifyMessage")
                if (notifyMessage?.success!!){
                    addNewMessage(notifyMessage)
                    onPlayVoice(notifyMessage!!)
                }
            } catch (e: Throwable) {
                MyLog.e("$TAG#WsManager-----onMessage exception:$e", e)
            }
        }

        override fun onMessage(bytes: ByteString?) {
            MyLog.e("${TAG}#WsManager-----onMessage")
        }

        override fun onReconnect() {
            MyLog.e("${TAG}#WsManager-----onReconnect")
        }

        override fun onClosing(code: Int, reason: String?) {
            MyLog.e("${TAG}#WsManager-----onClosing")
        }

        override fun onClosed(code: Int, reason: String?) {
            MyLog.e("${TAG}#WsManager-----onClosed")
        }

        override fun onFailure(t: Throwable?, response: Response?) {
            MyLog.e("${TAG}#WsManager-----onFailure")
        }
    }



    private fun getServiceIntent(endpoint: String?): Intent {
        val resolution = viewModel.getResolution()
        MyLog.e("$TAG#getServiceIntent resolution:$resolution")
        return Intent(activity, StreamService::class.java)
            .putExtra("resolution", resolution)
            .putExtra("endpoint", endpoint).also {
            activity?.bindService(it, this, Context.BIND_AUTO_CREATE)
        }
    }

    private fun stopService() {
        context?.stopService(Intent(context, StreamService::class.java))
        mService = null
        try {
            context?.unbindService(this)
        } catch (e: Throwable) {
            MyLog.e("$TAG#stopService unbind exception:${e.message}", e)
        }
    }

    private fun render(viewState: MainViewModel.ViewState) {
        pushStreamBtn?.setText(viewState.streamButtonText)
    }

    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        MyLog.e("surfaceChanged")
        mService?.let {
            openGlView?.let { it1 -> it.setView(it1) }
            it.startPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        MyLog.e("surfaceDestroyed")
        mService?.let {
            context?.let { it1 -> it.setView(it1) }
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
        context?.unbindService(this)
    }

    override fun onStart() {
        super.onStart()
        getServiceIntent(null)
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

    private fun checkVisitIdForWsUrl():Boolean{
        val wsVisitId = wsUrl.substringAfterLast('/')
        val visitId = viewModel.getVisitId()
        MyLog.e("${TAG}#checkVisitIdForWsUrl equals:${wsVisitId}, ${visitId}")
        return TextUtils.equals(wsVisitId, visitId)
    }

    override fun start(context: Context?, endpoint: String) {
        StreamService.openGlView = mLayoutInflaterView?.findViewById(R.id.openglview)
        activity?.startService(getServiceIntent(endpoint))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventJob?.cancel()
        wsManager?.stopConnect()
        voicePlayer?.release()
        voicePlayer = null
        try {
            stopService()
        } catch (e: Exception) {
            MyLog.e("${TAG}#onDestroyView exception:$e", e)
        }
        rttScheduler.shutdownNow()
    }

    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}

