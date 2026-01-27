package com.anmi.camera.uvcplay.state

sealed class AddCaseState {
    object Idle : AddCaseState()                    // 默认未发请求
    object Loading : AddCaseState()                 // 请求中
    data class Success(val data: Any?) : AddCaseState()
    data class Error(val throwable: Throwable) : AddCaseState()
}
