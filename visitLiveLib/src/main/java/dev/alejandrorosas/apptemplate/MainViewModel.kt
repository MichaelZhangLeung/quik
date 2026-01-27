package dev.alejandrorosas.apptemplate

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.anmi.camera.uvcplay.api.VisitLiveModule
import com.anmi.camera.uvcplay.model.CaseModel
import com.base.MyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alejandrorosas.core.livedata.SingleLiveEvent
import dev.alejandrorosas.streamlib.StreamService
import dev.alejandrorosas.streamlib.StreamService.Companion.resolutions
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {
    // 当前选中索引
    private var selectedResolutionIndex: Int = 0
    private var resolution: String ="1280x720"
    private var visitId: String? = null
    private var caseModel: CaseModel? = null

    private val serviceLiveData = SingleLiveEvent<(StreamService) -> Unit>()
    val serviceLiveEvent: LiveData<(StreamService) -> Unit> get() = serviceLiveData

    private var viewState = MutableLiveData(ViewState())

    val command = MutableLiveData<Int>()
    fun sendCommand(value: Int) { command.value = value }


    // 外访案件状态 使用 SingleLiveEvent 避免状态重复触发（如屏幕旋转后）
    private val _caseState = SingleLiveEvent<CaseStatus>()
    val caseState: LiveData<CaseStatus> get() = _caseState

    private var streamCallback: IStreamStateCallback? = null
    fun getViewState(): LiveData<ViewState> = viewState

    fun notifyCaseEventStatus(success: Boolean, errorCode: Int) {
        MyLog.e("========>>>>>>>notifyCaseEventStatus:$success, $errorCode)")
        _caseState.value = CaseStatus(success, errorCode)
    }

    fun onStreamControlButtonClick() {
        withService {
            MyLog.e("========>>>>>>>onStreamControlButtonClick streamStatus:${StreamService.streamStatus}, isStreaming:${it.isStreaming}")
            if (it.isStreaming) {
//            if (StreamService.streamStatus == 1) {
                it.stopStream(true)
                MyLog.e("========>>>>>>>stopStream:$streamCallback)")
                streamCallback?.onStopStream()
                MyLog.e("========>>>>>>>stopStream:$it======>>>>)")
//                viewState.postValue(viewState.value!!.copy(streamButtonText = R.string.button_start_stream))
//            } else if (StreamService.streamStatus != 3){
            } else {
                val endpoint = sharedPreferences.getString("endpoint", null)

                if (endpoint.isNullOrBlank()) {
                    Toast.makeText(VisitLiveModule.getApp(), R.string.toast_missing_stream_url, Toast.LENGTH_LONG).show()
                    return@withService
                }

                if (StreamService.usbDeviceStatus != 1){
                    Toast.makeText(VisitLiveModule.getApp(), R.string.toast_device_disconnect, Toast.LENGTH_LONG).show()
                    return@withService
                }

                if (StreamService.streamStatus == 1){
                    Toast.makeText(VisitLiveModule.getApp(), R.string.toast_stream_connect, Toast.LENGTH_LONG).show()
                    return@withService
                }

                if (StreamService.streamStatus == 3){
                    Toast.makeText(VisitLiveModule.getApp(), R.string.toast_stream_connect_start, Toast.LENGTH_LONG).show()
                    return@withService
                }

                it.startStreamRtp(endpoint)

                MyLog.e("========>>>>>>>onStartStream:$streamCallback)")
                streamCallback?.onStartStream()
                MyLog.e("========>>>>>>>onStartStream:$it======>>>>)")
//                viewState.postValue(viewState.value!!.copy(streamButtonText = R.string.button_stop_stream))
            }
        }
    }

    fun getEndpoint(): String? {
        return sharedPreferences.getString("endpoint", null)
    }

    fun getResolution(): String {
        return sharedPreferences.getString("pref_resolution", "1280x720")!!
//        return resolution
    }

    fun getFps(): String {
        return sharedPreferences.getString("pref_fps", "30")!!
//        return resolution
    }

    /**
     * id来源：  1.fragment初始化时配置（新生成）
     *          2.推流成功后，外访已经结束则重新生成
     *          3.推流结束时开启外访，复用wsUrl路径中的visit_id(来源于1，2中同时配置的url)
     *          3.结束外访时，清空visit_id
     *
     *
     * web获取StreamService.visitId：开始外访成功后初始化web，web开始获取id。1.此时推流已开启，此时id来源1/2 2.推流未开启时，两种情况：推流从未开始，id来源1；
     *  推流关闭过，若上次推流中间经历过外访结束，来源3，若上次推流中间未经历过外访结束，来源1.2.3
     */
    fun setVisitId(id:String?) {
//       sharedPreferences.edit()
//           .putString("visitId", id)
//           .apply()
        MyLog.e("========>>>>>>>setVisitId:$id)", Throwable())
        visitId = id
        if (id == null) {
            StreamService.visitId = ""
        } else {
            StreamService.visitId = id
        }
    }

    fun getVisitId(): String? {
        return visitId
//        return sharedPreferences.getString("visitId", "")!!
    }

    fun setCaseModel(model:CaseModel?) {
//       sharedPreferences.edit()
//           .putString("visitId", id)
//           .apply()
        caseModel = model
        if (model == null){
            StreamService.caseId = ""
        } else {
            StreamService.caseId = model.case_id
        }
    }
    fun onSettingButtonClick(context:Context, callback: IServiceControlInterface) {
        showResolutionDialog(context, callback)
    }

    private fun showResolutionDialog(context: Context, callback: IServiceControlInterface) {
        val resolutionsArray = resolutions.toTypedArray()
        selectedResolutionIndex = resolutions.indexOf(getResolution())
        AlertDialog.Builder(context)
            .setTitle("选择分辨率")
            // 单选列表，第二个参数为默认选中项索引
            .setSingleChoiceItems(resolutionsArray, selectedResolutionIndex) { _, which ->
                selectedResolutionIndex = which
            }
            .setPositiveButton("确定") { dialog, _ ->
                // 用户点击确定后，按选中索引获取分辨率
                val resText = resolutionsArray[selectedResolutionIndex]
                resolution = resText.toString()
//                sharedPreferences.edit().putString("resolution", resText.toString()).apply()
//                val (w, h) = resText.split("x").map { it.toInt() }
                restartService(getEndpoint(), context, callback)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun restartService(endpoint:String?, context: Context, callback: IServiceControlInterface) {
        callback.stop(context)
        Handler(Looper.getMainLooper()).postDelayed({
            callback.start(context, endpoint)
        }, 1000L)
//        withService {
            // 停止当前推流
//            it.stopStream()
//            // 更新分辨率参数
//            streamService.setVideoResolution(width, height)
//            // 重新启动推流
//            streamService.startStream()
//        }
//        Toast.makeText(this, "分辨率已切换至 ${width}x$height", Toast.LENGTH_SHORT).show()
    }
    fun onSafeEjectButtonClick() {
        withService {
            Log.e("MainViewModel", "onSafeEjectButtonClick, isStreaming: ${it.isStreaming}")
            it.stopAnything()

//            if (it.isStreaming) {
//                it.stopStream(true)
//                viewState.postValue(viewState.value!!.copy(streamButtonText = R.string.button_start_stream))
//            }
        }
    }

    fun setStreamCallback(callback: IStreamStateCallback) {
        MyLog.e("========>>>>>>>setStreamCallback:$callback)")
        streamCallback = callback
    }

    private fun withService(block: (StreamService) -> Unit) {
        serviceLiveData.value = block
    }

    data class ViewState(
        val streamButtonText: Int = R.string.button_start_stream,
    )



    data class CaseStatus(val started: Boolean, val code: Int)
}
