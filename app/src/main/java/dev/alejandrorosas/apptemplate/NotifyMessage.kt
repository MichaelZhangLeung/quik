package dev.alejandrorosas.apptemplate

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotifyMessage(
    val id: String,        // 唯一标识（如UUID）
    val type: String,   // 类型
    val success: Boolean,
    val data: Data,   // 消息内容
    val timestamp: Long,   // 时间戳（System.currentTimeMillis()）
) {
    // 不可变对象需重写equals/hashCode
    override fun equals(other: Any?) = other is NotifyMessage && id == other.id
    override fun hashCode() = id.hashCode()

    fun getFormattedTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
}

data class Data(
    val status: String,    // 如 "FACE_RECOGNITION_SUCCESS"
    val message: String    // 详细提示信息
) {
}
