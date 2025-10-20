package dev.alejandrorosas.streamlib;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.base.MyLog;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.List;

public class UsbDeviceManager {
    private static final String TAG = "[UsbDeviceManager]";
    private static final String ACTION_USB_PERMISSION = "com.anmi.workspace.USB_PERMISSION";

    private static UsbDeviceManager usbDeviceManager;
    Context mContext;
    List<UsbSerialDriver> availableDrivers;
    UsbDeviceConnection connection;
    UsbManager usbManager;
    UsbSerialDriver driver;
    UsbSerialPort controlPort;
    UsbSerialPort micPort;
    UsbStateCallBack usbStateCallBack;

    int baudrate = 921600; // Your desired baudrate
    public static final boolean ENABLE_SERIAL_PORT_MIC = false;//todo 串口ai眼镜mod 串口设备方案开关
//    int baudrate = 512000; // Your desired baudrate


    private UsbDeviceManager() {
    }

    public static UsbDeviceManager getInstance() {
        if (usbDeviceManager == null) {
            usbDeviceManager = new UsbDeviceManager();
        }
        return usbDeviceManager;
    }

    public void saveDeviceInfo(String boxId,String boxBatch){
//        SpUtils.setSharedStringData(Config.KEY_BOX_IMEI,boxId);
//        SpUtils.setSharedStringData(Config.KEY_BOX_BATCH,boxBatch);
    }

    public void initDevice(Context context) {

        if (!ENABLE_SERIAL_PORT_MIC){
            return;
        }

        mContext = context;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbSerialProber prober = UsbSerialProber.getDefaultProber();
        availableDrivers = prober.findAllDrivers(usbManager);
        if (availableDrivers == null || availableDrivers.isEmpty()) {
            UsbUtils.toast(context.getString(R.string.toast_device_empty));
            return;
        }
        checkPermission();
    }

    public void setUsbStateCallBack(UsbStateCallBack callBack) {
        if (!ENABLE_SERIAL_PORT_MIC){
            return;
        }
        usbStateCallBack = callBack;
    }

    private void openConnect(boolean isNeedRequestPermission) {
        try {
            if (availableDrivers == null || availableDrivers.isEmpty()) {
                UsbUtils.toast(mContext.getString(R.string.toast_device_empty));
                if (usbStateCallBack != null) {
                    usbStateCallBack.setUsbState(false,isNeedRequestPermission);
                }
                return;
            }
            log("设备列表：" + availableDrivers);
            driver = availableDrivers.get(0);
//            log("选择设备：" + driver);
            connection = usbManager.openDevice(driver.getDevice());
            if (connection == null) {
                log("连接失败 ！！");
                return;
            }

            micPort = driver.getPorts().get(0);
            log("micPort：" + micPort);
            if (!micPort.isOpen()) {
                micPort.open(connection);
                micPort.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                micPort.setDTR(true);
                micPort.setRTS(true);
                log("micPort opened======");
            }

            controlPort = driver.getPorts().get(1);
            if (!controlPort.isOpen()) {
                controlPort.open(connection);
                controlPort.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_ODD);
                controlPort.setDTR(true);
                controlPort.setRTS(true);
                log("controlPort opened======");
            }

            if (usbStateCallBack != null) {
                usbStateCallBack.setUsbState(true,isNeedRequestPermission);
            }

        } catch (Exception e) {
            MyLog.e(TAG+ "openConnect:" + Log.getStackTraceString(e));
            if (mContext != null){
                UsbUtils.toast(mContext.getString(R.string.toast_device_serial_connect_error));
            }
        }

    }

    /**
     * 检查usb权限
     */
    private void checkPermission() {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        MyLog.e(TAG+ "checkPermission, device size:" + usbManager.getDeviceList().size());
        ArrayList<Boolean> permissions = new ArrayList<>();
        boolean isNeedRequestPermission = false;
        for (final UsbDevice usbDevice : usbManager.getDeviceList().values()) {

            if (!UsbUtils.isUsbMicrophone(usbDevice)){
                continue;
            }

            MyLog.e(TAG+ "checkPermission device:" + UsbUtils.getDeviceName(usbDevice) + ", hasPermission:" + usbManager.hasPermission(usbDevice));


//            if (!usbManager.hasPermission(usbDevice)) {
//                isNeedRequestPermission = true;
//                usbManager.requestPermission(usbDevice, mPermissionIntent);
//            } else{
//                permissions.add(true);
//            }
        }
        openConnect(false);
//       if (permissions.size()==usbManager.getDeviceList().size()){
//           openConnect(isNeedRequestPermission);
//       }
    }

    public UsbSerialPort getControlPort() {
        if (controlPort == null || !controlPort.isOpen()) {
            return null;
        }
        return controlPort;
    }

    public UsbSerialPort getMicPort() {
        if (micPort == null || !micPort.isOpen()) {
            return null;
        }
        return micPort;
    }

    public void clearPorts() {
        micPort = null;
        controlPort = null;
        if (usbStateCallBack != null) {
            usbStateCallBack.setUsbState(false,false);
        }
    }

    public interface UsbStateCallBack {
        void setUsbState(boolean hasConnected,boolean isNeedRequestPermission);
    }


    private void log(String msg){
        MyLog.e(TAG + msg);
    }


    private void loge(String msg, Throwable e){
        MyLog.e(TAG + msg, e);
    }

    // Thread that reads from usb-serial and pushes raw bytes into pcmQueue.
    // You must parse the board protocol here: some firmwares send custom headers.
//    private class ReadThread extends Thread implements SerialInputOutputManager.Listener {
//        private final UsbSerialPort port;
//        private volatile boolean running = true;
//
//        ReadThread(UsbSerialPort p) {
//            this.port = p;
//            setName("UsbReadThread");
//        }
//
//        @Override
//        public void run() {
//            // Instead of using SerialInputOutputManager callback, we'll read in blocking loop
//            final int READ_BUF = 4096;
//            byte[] buf = new byte[READ_BUF];
//            while (running) {
//                try {
//                    int len = port.read(buf, 1000); // timeout 1s
//                    if (len > 0) {
//                        byte[] chunk = new byte[len];
//                        System.arraycopy(buf, 0, chunk, 0, len);
//                        // If your board wraps PCM in headers, parse here and only enqueue raw PCM
//                        onUsbDataReceived(chunk);
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "read error", e);
//                    break;
//                }
//            }
//            Log.i(TAG, "ReadThread exiting");
//        }
//
//        private void onUsbDataReceived(byte[] data) {
//            // Example: assume the device sends pure PCM16LE audio bytes continuously.
//            // If your device sends packet headers, strip them here.
//            try {
//                // queue may block if encoder is slow; you can implement a bounded policy
//                pcmQueue.put(data);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        void shutdown() {
//            running = false;
//            try { join(500); } catch (InterruptedException ignored) {}
//        }
//
//        @Override public void onRunError(Exception e) {}
//        @Override public void onNewData(byte[] data) { onUsbDataReceived(data); }
//    }
//
    // AAC encoder thread: consumes PCM chunks from pcmQueue, feeds to MediaCodec, and writes ADTS-wrapped AAC to file
//    private class AacEncoderThread extends Thread {
//        private volatile boolean running = true;
//        private MediaCodec codec;
////        private FileOutputStream fos;
//
//        // keep track of sample count to generate PTS
//        private long totalPcmBytesConsumed = 0;
//
//        AacEncoderThread() {}
//
//        @Override
//        public void run() {
//            try {
////                fos = openFileOutput("output.aac", Context.MODE_PRIVATE);
//
//                MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
//                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//                format.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
//                // optional: format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
//
//                codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
//                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//                codec.start();
//
//                ByteBuffer[] inputBuffers = codec.getInputBuffers();
//                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//
//                while (running) {
//                    // get next PCM chunk (blocking)
//                    byte[] pcm = pcmQueue.take();
//
//                    // feed PCM to encoder (may need to split into smaller chunks)
//                    int offset = 0;
//                    while (offset < pcm.length) {
//                        int inIndex = codec.dequeueInputBuffer(10000);
//                        if (inIndex >= 0) {
//                            ByteBuffer inputBuf = inputBuffers[inIndex];
//                            inputBuf.clear();
//                            int toPut = Math.min(inputBuf.capacity(), pcm.length - offset);
//                            inputBuf.put(pcm, offset, toPut);
//
//                            long presentationTimeUs = computePresentationTimeUs(totalPcmBytesConsumed);
//                            codec.queueInputBuffer(inIndex, 0, toPut, presentationTimeUs, 0);
//
//                            totalPcmBytesConsumed += toPut;
//                            offset += toPut;
//                        } else {
//                            // no input buffer available, retry a bit
//                            Thread.yield();
//                        }
//                    }
//
//                    // drain output
//                    int outIndex = codec.dequeueOutputBuffer(info, 0);
//                    while (outIndex >= 0) {
//                        ByteBuffer encoded = outputBuffers[outIndex];
//                        byte[] aac = new byte[info.size + 7]; // 7 bytes ADTS header
//                        // add ADTS header
//                        addAdtsHeader(aac, info.size + 7);
//                        encoded.position(info.offset);
//                        encoded.limit(info.offset + info.size);
//                        encoded.get(aac, 7, info.size);
//
//                        // send or save the frame
//                        sendEncodedAacFrame(aac, info.presentationTimeUs);
//
//                        codec.releaseOutputBuffer(outIndex, false);
//                        outIndex = codec.dequeueOutputBuffer(info, 0);
//                    }
//                }
//
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } catch (IOException e) {
//                Log.e(TAG, "Encoder IO error", e);
//            } catch (Exception e) {
//                Log.e(TAG, "Encoder error", e);
//            } finally {
//                try {
//                    if (codec != null) {
//                        try { codec.stop(); } catch (Exception ignored) {}
//                        try { codec.release(); } catch (Exception ignored) {}
//                        codec = null;
//                    }
//                    if (fos != null) {
//                        fos.close();
//                        fos = null;
//                    }
//                } catch (IOException ignored) {}
//            }
//        }
//
//        void shutdown() {
//            running = false;
//            this.interrupt();
//            try { join(500); } catch (InterruptedException ignored) {}
//        }
//
//        private long computePresentationTimeUs(long pcmBytesConsumed) {
//            // pcmBytesConsumed is bytes; convert to samples: bytes / (channels * bytesPerSample)
//            long samples = pcmBytesConsumed / (CHANNEL_COUNT * PCM_FRAME_BYTES);
//            return (samples * 1000000L) / SAMPLE_RATE;
//        }
//
//        private void addAdtsHeader(byte[] packet, int packetLen) {
//            // packetLen includes ADTS header length
//            int profile = 2; // AAC LC
//            int freqIdx = getFrequencyIndex(SAMPLE_RATE); // 4 for 48000
//            int chanCfg = CHANNEL_COUNT;
//
//            // syncword 0xFFF (12 bits)
//            packet[0] = (byte) 0xFF;
//            packet[1] = (byte) 0xF1; // 1111 0001 : MPEG-4, layer 0, protection absent
//            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
//            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
//            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
//            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
//            packet[6] = (byte) 0xFC;
//        }
//
//        private int getFrequencyIndex(int sampleRate) {
//            switch (sampleRate) {
//                case 96000: return 0;
//                case 88200: return 1;
//                case 64000: return 2;
//                case 48000: return 3;
//                case 44100: return 4;
//                case 32000: return 5;
//                case 24000: return 6;
//                case 22050: return 7;
//                case 16000: return 8;
//                case 12000: return 9;
//                case 11025: return 10;
//                case 8000:  return 11;
//                default: return 4; // fallback to 44100
//            }
//        }
//
//        private void sendEncodedAacFrame(byte[] aacAdts, long ptsUs) {
//            // Example: write to file. For RTMP push, replace this with your RTMP library call.
//            try {
//                if (fos != null) {
//                    fos.write(aacAdts);
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "write aac file error", e);
//            }
//
//            // --- RTMP integration point ---
//            // e.g. rtmpClient.sendAudio(aacPayloadWithoutAdts, ptsUs);
//            // Important: many RTMP libraries expect raw AAC (no ADTS) and an initial AudioSpecificConfig
//            // to be sent as extradata. If you integrate RTMP, send AudioSpecificConfig first.
//        }
//    }
}
