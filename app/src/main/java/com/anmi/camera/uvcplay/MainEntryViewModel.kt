package com.anmi.camera.uvcplay

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anmi.camera.uvcplay.data.CaseDataRepository
import com.anmi.camera.uvcplay.model.CaseModel
import com.base.MyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alejandrorosas.apptemplate.R
import dev.alejandrorosas.core.livedata.SingleLiveEvent
import dev.alejandrorosas.streamlib.StreamService
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainEntryViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val repository: CaseDataRepository
) : ViewModel() {

    private val _cases = MutableLiveData<List<CaseModel>>()
    val cases: LiveData<List<CaseModel>> = _cases

    private val serviceLiveData = SingleLiveEvent<(StreamService) -> Unit>()
    val serviceLiveEvent: LiveData<(StreamService) -> Unit> get() = serviceLiveData

    private var viewState = MutableLiveData(ViewState())


    fun loadPosts() {
        viewModelScope.launch {
            try {
                val result = repository.getCases()
//                _posts.value = posts//需主线程调用 立即通知
                if (result != null){
                    _cases.postValue(result!!)//异步 主线程队列排队执行通知
                }
            } catch (e: Throwable) {
               MyLog.e("==========#getCases exception:$e" )
            }
        }
    }

    fun getViewState(): LiveData<ViewState> =viewState

    fun onStreamControlButtonClick() {
        withService {

        }
    }

    fun onSettingButtonClick(context:Context) {
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

//    fun setStreamCallback(callback: IStreamStateCallback) {
//        streamCallback = callback
//    }

    private fun withService(block: (StreamService) -> Unit) {
        serviceLiveData.value = block
    }

    data class ViewState(
        val streamButtonText: Int = R.string.button_start_stream,
    )
}
