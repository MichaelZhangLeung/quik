package dev.alejandrorosas.apptemplate

import androidx.recyclerview.widget.DiffUtil

class MessageDiffCallback : DiffUtil.ItemCallback<NotifyMessage>() {


    companion object {
        const val PAYLOAD_READ_STATUS = "read_status"
    }
    override fun areItemsTheSame(oldItem: NotifyMessage, newItem: NotifyMessage) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: NotifyMessage, newItem: NotifyMessage) =
        oldItem == newItem // 自动调用data class的equals方法

    // 可选：局部更新优化（如仅更新已读状态）
//    override fun getChangePayload(oldItem: NotifyMessage, newItem: NotifyMessage): Any? {
//        return if (oldItem.isRead != newItem.isRead)
//            PAYLOAD_READ_STATUS else null
//    }
}
