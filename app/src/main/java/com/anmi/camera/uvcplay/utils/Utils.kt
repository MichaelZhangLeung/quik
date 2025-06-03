package com.anmi.camera.uvcplay.utils

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import dev.alejandrorosas.apptemplate.AndroidApplication
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ID 生成工具类（单例模式）
 * 格式示例：20250523001722+6b29fc40caab11ee825c4a6bab007f44
 */
@SuppressLint("NewApi")
object Utils {

    fun wrapNotifyStatus(status:String):String{
        return "VISIT_$status"
    }
    fun readRTT(endPoint:String): Long{//"rtmp://dyai.duyansoft.com/live/stream5"
        val addr = InetAddress.getByName(endPoint)
        val start = System.currentTimeMillis()
        val ok = addr.isReachable(1000)         // Android 会先尝试 ICMP，再降级到 TCP :contentReference[oaicite:2]{index=2}
        return System.currentTimeMillis() - start
    }

    /**
     * 给 View 添加防抖点击监听器：
     * intervalMillis 毫秒内只响应一次点击
     */
    fun View.setDebounceClickListener(intervalMillis: Long = 500L, onClick: (View) -> Unit) {
        var lastClickTime = 0L
        setOnClickListener { v ->
            val current = SystemClock.elapsedRealtime()
            if (current - lastClickTime > intervalMillis) {
                lastClickTime = current
                onClick(v)
            }
        }
    }

    fun toast(msg:String) {
        Toast.makeText(AndroidApplication.app, msg, Toast.LENGTH_SHORT).show()
    }

    // 默认配置
    private var dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var uuidUpperCase: Boolean = false
    private var uuidShorten: Int = -1 // -1 表示不截取

    /**
     * 初始化全局配置（可选）
     * @param pattern 时间格式（默认：yyyyMMddHHmmss）
     * @param zoneIdStr 时区ID（默认：系统时区）
     * @param uuidUpperCase 是否大写UUID（默认：小写）
     * @param uuidShorten 截取UUID前N位（默认：不截取）
     */
    fun config(
        pattern: String = "yyyyMMddHHmmss",
        zoneIdStr: String = ZoneId.systemDefault().id,
        uuidUpperCase: Boolean = false,
        uuidShorten: Int = -1
    ) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
        zoneId = ZoneId.of(zoneIdStr)
        this.uuidUpperCase = uuidUpperCase
        this.uuidShorten = uuidShorten
    }

    fun generateVisitId(): String {
        return generate()
    }

    /**
     * 生成组合ID
     * @param prefix 自定义前缀（可选）
     */
    fun generate(prefix: String = ""): String {
        // 时间部分
        val timestamp = LocalDateTime.now(zoneId).format(dateTimeFormatter)
        
        // UUID处理
        val uuid = processUuid(UUID.randomUUID().toString())
        
        // 组合结果
        return if (prefix.isEmpty()) "$timestamp$uuid" else "${prefix}$timestamp$uuid"
    }

    private fun processUuid(rawUuid: String): String {
        return rawUuid.replace("-", "")
            .let { if (uuidUpperCase) it.uppercase() else it.lowercase() }
            .let { if (uuidShorten > 0) it.take(uuidShorten) else it }
    }
}
