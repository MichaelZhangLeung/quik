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

    private val serviceLiveData = SingleLiveEvent<(StreamService) -> Unit>()
    val serviceLiveEvent: LiveData<(StreamService) -> Unit> get() = serviceLiveData

    private var viewState = MutableLiveData(ViewState())

    private var streamCallback: IStreamStateCallback? = null

    fun getViewState(): LiveData<ViewState> = viewState

    fun onStreamControlButtonClick() {
        withService {
            if (it.isStreaming) {
                it.stopStream(true)
                streamCallback?.onStopStream()
                viewState.postValue(viewState.value!!.copy(streamButtonText = R.string.button_start_stream))
            } else {
                val endpoint = sharedPreferences.getString("endpoint", null)

                if (endpoint.isNullOrBlank()) {
                    Toast.makeText(AndroidApplication.app, R.string.toast_missing_stream_url, Toast.LENGTH_LONG).show()
                    return@withService
                }
                it.startStreamRtp(endpoint)
                streamCallback?.onStartStream()
                viewState.postValue(viewState.value!!.copy(streamButtonText = R.string.button_stop_stream))
            }
        }
    }

    fun getEndpoint(): String? {
        return sharedPreferences.getString("endpoint", null)
    }

    fun getResolution(): String {
//        return sharedPreferences.getString("resolution", null)
        return resolution
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
        streamCallback = callback
    }

    private fun withService(block: (StreamService) -> Unit) {
        serviceLiveData.value = block
    }

    data class ViewState(
        val streamButtonText: Int = R.string.button_start_stream,
    )
}
