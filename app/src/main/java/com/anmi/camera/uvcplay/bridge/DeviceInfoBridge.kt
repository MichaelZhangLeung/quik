package com.anmi.camera.uvcplay.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.preference.PreferenceManager
import com.base.MyLog
import dev.alejandrorosas.apptemplate.AndroidApplication
import dev.alejandrorosas.streamlib.StreamService
import org.json.JSONObject

class DeviceInfoBridge(private val context: Context) {
    companion object {
        const val TAG = "[DeviceInfoBridge]"
        const val BRIDGE_NAME = "DeviceChannel"

        fun readRtmpUrl(): String {
            return PreferenceManager.getDefaultSharedPreferences(AndroidApplication.app!!).getString("endpoint", "").toString()
        }
    }

    /** 标记此方法可被 JS 调用 */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        val pushStatus = checkPushStatus()   // 1为推送中
        val usbStatus  = checkUsbStatus()    // 1为已连接
        val url  = readRtmpUrl()
        MyLog.e("[DeviceInfoBridge] #getDeviceInfo push_status: $pushStatus, usb_status: $usbStatus, visitId: ${StreamService.visitId}, caseId: ${StreamService.caseId}, url: ${url}")
        return JSONObject().apply {
            put("visit_id", StreamService.visitId)
            put("case_id", StreamService.caseId)
            put("rtmp_url", url)
            put("push_status", pushStatus)
            put("usb_status", usbStatus)
        }.toString()
    }

    private fun checkPushStatus(): Int = StreamService.streamStatus
    private fun checkUsbStatus(): Int = StreamService.usbDeviceStatus
}
