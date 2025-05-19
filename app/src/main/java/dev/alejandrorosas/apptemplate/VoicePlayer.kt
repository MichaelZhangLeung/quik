package dev.alejandrorosas.apptemplate

import android.media.MediaPlayer
import android.content.Context
import androidx.annotation.RawRes

class VoicePlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    /** 播放 raw 目录下的 WAV 文件 */
    fun play(@RawRes resId: Int) {
        // 如果已有实例，先释放
        mediaPlayer?.release()

        // 创建 MediaPlayer 并准备播放
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            setOnCompletionListener {
                // 播放完成时释放资源
                it.release()
                mediaPlayer = null
            }
            setOnErrorListener { mp, what, extra ->
                // 播放出错也释放资源
                mp.release()
                mediaPlayer = null
                true
            }
            start()
        }
    }

    /** 在不需要时手动释放 */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

