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
import com.anmi.camera.uvcplay.state.AddCaseState
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

    // 用 MutableLiveData；也可以换成 MutableStateFlow, LiveData 也能 work
    private val _addCaseState = MutableLiveData<AddCaseState>(AddCaseState.Idle)
    val addCaseState: LiveData<AddCaseState> = _addCaseState

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
    fun addCase(caseModel: CaseModel, visitId:String) {
        // 先发一个 Loading 状态
        _addCaseState.value = AddCaseState.Loading
        viewModelScope.launch {
            try {
                val result:Any? = repository.addCase(caseModel, visitId)
                MyLog.e("==========#addCase result:$result")
                _addCaseState.postValue(AddCaseState.Success(result))
            } catch (e: Throwable) {
               MyLog.e("==========#addCase exception:$e")
                _addCaseState.postValue(AddCaseState.Error(e))
            } finally {
            }
        }
    }
    fun endVisit(caseModel: CaseModel?, visitId:String) {
        viewModelScope.launch {
            try {
                val result:Any? = repository.endVisit(caseModel, visitId)
                MyLog.e("==========#endVisit result:$result")
            } catch (e: Throwable) {
               MyLog.e("==========#endVisit exception:$e")
            } finally {
            }
        }
    }
    fun idle() {
        _addCaseState.postValue(AddCaseState.Idle)
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
