package com.anmi.camera.uvcplay.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 启动外放参数
 */
@Parcelize
data class VisitParams(
    val caseId: String? = null,        // 业务案件ID
    val visitId: String? = null,       // 访视ID
    val collectorId: String? = null,   // 当前催收员/采集者ID
    val rtmpUrl: String? = null,       // 预设 RTMP 推流地址（可为空）
    val autoStartStream: Boolean = false, // 是否启动后直接开始推流
    val extra: String? = null          // 备用 JSON 字符串或其他小文本
) : Parcelable
