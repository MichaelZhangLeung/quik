package dev.alejandrorosas.streamlib;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.util.Log;

import com.base.MyLog;
import com.hoho.android.usbserial.driver.UsbSerialPort;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class StreamBridge {
    private static final String TAG = "[StreamBridge]";
    static StreamBridge streamBridge;
    BlockingDeque<byte[]> micQueue;//mic队列

    public static volatile boolean isStartRead = false;
    public boolean startRecord = false;
    FileOutputStream fos1 = null;

    @SuppressLint("NewApi")
    private final ExecutorService workThreadExecutor = Executors.newWorkStealingPool();


    private StreamBridge() {
    }

    public static StreamBridge getInstance() {
        if (streamBridge == null) {
            streamBridge = new StreamBridge();
        }
        return streamBridge;
    }

    public BlockingDeque<byte[]> getMicQueue() {
        return micQueue;
    }

    public void startWork() {
        MyLog.e(TAG+"startWork:"+isStartRead);

        if (isStartRead){
            return;
        }
        isStartRead = true;
        startRecord = true;
        micQueue = new LinkedBlockingDeque<>(5000);
        handleMicPortData();
    }

    public void stopWork() {
        MyLog.e(TAG+"stopWork:"+isStartRead);
        startRecord =false;
        if (fos1 != null){
            try {
                fos1.flush();
                fos1.close();
            } catch (IOException e) {
                MyLog.e(TAG+" close record exception:"+e);
            } finally {
                fos1 = null;
            }
        }
        try {
            if (!isStartRead){
                return;
            }
            isStartRead = false;
            if (micQueue !=null){
                micQueue.clear();
                micQueue = null;
            }
        } catch (Exception e) {
            MyLog.e(TAG+ "stopWork error:" + Log.getStackTraceString(e));
        }
    }

    public void handleMicPortData() {

        if (fos1 == null) {
            try {
                fos1 = new FileOutputStream("/sdcard/Download/micOrigin.pcm");
            } catch (Exception e) {
            }
        }
        UsbSerialPort micPort = UsbDeviceManager.getInstance().getMicPort();
        if (micPort == null){
            return;
        }
        workThreadExecutor.submit(() -> {
            SystemClock.sleep(2000);
            MyLog.e(TAG + "#handleEarPortData start ======>>>>>>");
            final int frameSize = 320;
            byte[] readBuf = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (isStartRead) {
                try {
//                    int size = micPort.read(readBuf, 50); //
                    int size = micPort.read(readBuf, 5); //
                    MyLog.e(TAG + "#handleEarPortData read size ======>>>>>>" + size + ", readBuf:" + Arrays.toString(readBuf));
                    if (size > 0) {

                        if (startRecord){
                            fos1.write(readBuf, 0, size);
//                            fos1.flush();
                        }

                        // append
                        baos.write(readBuf, 0, size);
                        byte[] buf = baos.toByteArray();
                        int offset = 0;
                        while (buf.length - offset >= frameSize) {
                            byte[] frame = new byte[frameSize];
                            System.arraycopy(buf, offset, frame, 0, frameSize);
                            offset += frameSize;

//                             非阻塞入队：若满，丢最旧再尝试入队（保留最新） todo 串口ai眼镜mod 本地录音测试需要注释入队代码
                            boolean offered = micQueue.offerLast(frame);
                            if (!offered) {
                                micQueue.pollFirst();
                                micQueue.offerLast(frame);
                            }
                        }
                        // keep remainder
                        baos.reset();
                        if (offset < buf.length) {
                            baos.write(buf, offset, buf.length - offset);
                        }
                        MyLog.d(TAG + "micQueue size:" + micQueue.size());
                    }
                } catch (IOException e) {
                    MyLog.e(TAG + Log.getStackTraceString(e));
                    isStartRead = false;
                    break;
                }
            }

        });
    }

}
