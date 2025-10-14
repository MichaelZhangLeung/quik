package dev.alejandrorosas.apptemplate

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anmi.camera.uvcplay.utils.Utils.wrapNotifyStatus

class NotifyMessageAdapter : ListAdapter<NotifyMessage, NotifyMessageAdapter.MessageViewHolder>(MessageDiffCallback()){

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusToRes = mapOf(
            wrapNotifyStatus("FACE_RECOGNITION_SUCCESS") to R.string.text_message_face_ok,
            wrapNotifyStatus("FACE_RECOGNITION_FAILURE") to R.string.text_message_face_fail,
            wrapNotifyStatus("ENVIRONMENT_RECOGNITION_OLD") to R.string.text_message_environment_old,
            wrapNotifyStatus("AUDIO_EMOTION_ANALYSIS_EXCITE") to R.string.text_message_emotional,
            wrapNotifyStatus("VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL") to R.string.text_message_behavior_abnormal
        )

        private fun tranStatusToText(context: Context, status: String): CharSequence =
            statusToRes[status]?.let { context.getString(it) } ?: status

        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: NotifyMessage) {
//            tvTitle.text = when (message.data.status) {
//                wrapNotifyStatus("FACE_RECOGNITION_SUCCESS") -> R.string.text_message_face_ok
//                wrapNotifyStatus("FACE_RECOGNITION_FAILURE") -> "人脸识别失败"
//                wrapNotifyStatus("ENVIRONMENT_RECOGNITION_OLD") -> "环境老旧"
//                wrapNotifyStatus("AUDIO_EMOTION_ANALYSIS_EXCITE") -> "情绪激动"
//                wrapNotifyStatus("VIDEO_EMOTION_BEHAVIOR_ANALYSIS_ABNORMAL") -> "异常行为"
//                else                        -> message.data.status
//            }
            tvTitle.text = tranStatusToText(itemView.context, message.data.status)
            tvContent.text = message.data.message
            tvTime.text = message.getFormattedTime()
        }
    }
    override fun submitList(list: List<NotifyMessage>?) {
        super.submitList(list?.sortedByDescending { it.timestamp })
    }
    fun clearList() {
        super.submitList(null)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 处理局部更新
//    override fun onBindViewHolder(holder: MessageViewHolder, position: Int,
//                                  payloads: MutableList<Any>) {
//        if (payloads.isNullOrEmpty()) {
//            super.onBindViewHolder(holder, position, payloads)
//        } else {
//            payloads.forEach { payload ->
//                if (payload == MessageDiffCallback.PAYLOAD_READ_STATUS) {
//                    holder.updateReadStatus(getItem(position).isRead)
//                }
//            }
//        }
//    }
}
