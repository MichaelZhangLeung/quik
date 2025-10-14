package dev.alejandrorosas.streamlib;
import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;
import android.util.Log;

import com.base.MyLog;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PcmPlayerUtils {
    @SuppressLint("NewApi")
    private ExecutorService singleExecutor = Executors.newWorkStealingPool();

    private AudioTrack audioTrack;

    public void playPcm(String pcmFilePath, int sampleRate, int channelConfig, int audioFormat) {
        try {
            // 读取 PCM 文件
            byte[] pcmData = readPcmFile(pcmFilePath);

            // 计算最小缓冲区大小
            int minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    audioFormat
            );

            // 创建 AudioTrack（使用 MODE_STREAM 流式播放）
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
            );

            // 开始播放
            audioTrack.play();

            // 写入 PCM 数据
            audioTrack.write(pcmData, 0, pcmData.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPlayback() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    private byte[] readPcmFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        fis.close();
        return bos.toByteArray();
    }

    public static final String earSendPath = "/sdcard/Download/ear_send.pcm";
    public void sendStream(UsbSerialPort port) {
        singleExecutor.submit(() -> {
            MyLog.e( "发送文件:" + port);
            File pcmFile = new File(earSendPath);
            if (!pcmFile.exists()) {
                UsbUtils.toast("文件不存在");
                // 文件不存在处理逻辑
                return;
            }
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(pcmFile);
                int size = 640;
                byte[] buffer = new byte[size];
                int bytesRead;
                while (inputStream.read(buffer)>0) {
                    MyLog.e( "发送文件:");
                    port.write(buffer,0);
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                // 异常处理逻辑
                MyLog.e(Log.getStackTraceString(e));

            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    // 异常处理逻辑
                }
            }
        });

    }

    public static boolean copyNewFile(String oldPath, String newPath, boolean append) {
        if (TextUtils.isEmpty(oldPath)) return false;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                inputStream = new FileInputStream(oldPath); //读入原文件
                outputStream = new FileOutputStream(newPath, append);
                byte[] buffer = new byte[2048];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                return true;
            }
        } catch (Exception e) {
            MyLog.e("复制单个文件操作出错");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
