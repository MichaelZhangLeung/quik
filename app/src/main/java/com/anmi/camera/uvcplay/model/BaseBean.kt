package com.anmi.camera.uvcplay.model

import java.io.Serializable

class BaseBean<T> : Serializable {
    companion object {
        const val CODE_MSG_ERROR = 1000
        const val CODE_LOGIN_OUT = 2000
        const val CODE_SUCCESS = 200
    }

    var code: Int? = null //返回码
    var data: T? = null//成功返回内容
    var msg = ""//返回信息
    var success = false//是否成功
    var time = ""

    fun toResult(): T? {
        if (CODE_SUCCESS == code && success) {
            return data
        } else {
            throw HttpThrowable(code!!, msg)
        }
    }

    fun toCode(): Int? {
        return code
    }
}
