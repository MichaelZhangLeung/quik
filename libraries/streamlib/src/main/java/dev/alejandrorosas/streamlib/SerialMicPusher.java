package dev.alejandrorosas.streamlib;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.SystemClock;

import com.base.MyLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialMicPusher {

    public static final String TAG = "[SerialMicPusher]" ;
    @SuppressLint("NewApi")
    private final ExecutorService workThreadExecutor = Executors.newWorkStealingPool();
//    private final RtmpClient rtmpClient;
    private final RtmpUSB rtmpUSB;
//    private ConcurrentLinkedDeque<byte[]> micQueue;
    private BlockingDeque<byte[]> micQueue;
    private MediaCodec audioEncoder;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isEncoding = false;

    private long totalPcmBytesConsumed = 0L;
    private static final int SAMPLE_RATE = 16000; // 请保持与你实际采样率一致
    private static final int CHANNEL_COUNT = 1;
    private static final int PCM_BYTES_PER_SAMPLE = 2; // 16-bit -> 2 bytes

    public SerialMicPusher(RtmpUSB rtmpUSB) {
        this.rtmpUSB = rtmpUSB;
    }

    public void startEncoding() throws IOException {
        // 初始化 AAC 编码器
        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                16000,  // 采样率 16k
                1       // 单声道
        );
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000); // 比特率可调整
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);

        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoder.start();
        bufferInfo = new MediaCodec.BufferInfo();
        isEncoding = true;

        // 开线程读取 micQueue 并编码推流
        workThreadExecutor.submit(() -> {
            SystemClock.sleep(1000);
            micQueue = StreamBridge.getInstance().getMicQueue();
            MyLog.d(TAG + "#startEncoding thread start===>>>"+(micQueue != null? micQueue.size():null));
            encodeLoop();
        }) ;
    }

    private void encodeLoop() {
        try {
            while (isEncoding) {
                if (micQueue == null){
                    micQueue = StreamBridge.getInstance().getMicQueue();
                    continue;
                }

                byte[] pcmData = micQueue.take(); // 阻塞等待串口PCM数据
//                MyLog.e(TAG + "#encodeLoop size:"+(pcmData != null?pcmData.length:null));

                if (pcmData == null || pcmData.length == 0){
                    continue;
                }

                pcmData = processPcmFrame(pcmData);

                // Feed PCM possibly in chunks if input buffer smaller
                int offset = 0;
                while (offset < pcmData.length) {
                    int inIndex = audioEncoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer inBuffer = audioEncoder.getInputBuffer(inIndex);
                        inBuffer.clear();
                        int toPut = Math.min(inBuffer.capacity(), pcmData.length - offset);
                        inBuffer.put(pcmData, offset, toPut);

                        long ptsUs = computePresentationTimeUs(totalPcmBytesConsumed);
                        audioEncoder.queueInputBuffer(inIndex, 0, toPut, ptsUs, 0);

                        totalPcmBytesConsumed += toPut;
                        offset += toPut;
                    } else {
                        Thread.yield();
                    }
                }

                // === 取出编码后的 AAC 数据并发送给 RTMP（避免对同一 ByteBuffer 重复读取） ===
                int outIndex;
                while ((outIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0)) >= 0) {
                    ByteBuffer outBuffer = audioEncoder.getOutputBuffer(outIndex);
                    if (outBuffer == null) {
                        audioEncoder.releaseOutputBuffer(outIndex, false);
                        continue;
                    }

                    // create a readable slice without modifying the original buffer state
                    ByteBuffer slice = outBuffer.duplicate();
                    slice.position(bufferInfo.offset);
                    slice.limit(bufferInfo.offset + bufferInfo.size);

                    // copy payload to a byte[] so we can safely wrap and send
                    byte[] aacPayload = new byte[bufferInfo.size];
                    slice.get(aacPayload);

                    // prepare a ByteBuffer for sending (position=0, limit=size)
                    ByteBuffer toSend = ByteBuffer.wrap(aacPayload);

                    // prepare a copy of BufferInfo where offset is 0 (since toSend starts at 0)
                    MediaCodec.BufferInfo sendInfo = new MediaCodec.BufferInfo();
                    sendInfo.set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);

                    try {
                        // send via your RtmpUSB wrapper which delegates to RtmpClient#sendAudio
                        rtmpUSB.sendAudio(toSend, sendInfo);
                    } catch (Exception e) {
                        MyLog.e(TAG + " sendAudio exception:" + e, e);
                    }

                    audioEncoder.releaseOutputBuffer(outIndex, false);
                }
            }
        } catch (Throwable e) {
            MyLog.e(TAG + "#encodeLoop exception:" +e, e);
            Thread.currentThread().interrupt();
        }
    }

    private long computePresentationTimeUs(long pcmBytesConsumed) {
        long samples = pcmBytesConsumed / (CHANNEL_COUNT * PCM_BYTES_PER_SAMPLE);
        return (samples * 1000000L) / SAMPLE_RATE;
    }

//    private void encodeLoop() {
//        try {
//            while (isEncoding) {
//                if (micQueue == null){
//                    micQueue = StreamBridge.getInstance().getMicQueue();
//                    continue;
//                }
//
//                byte[] pcmData = micQueue.take(); // 阻塞等待串口PCM数据
////                MyLog.e(TAG + "#encodeLoop size:"+(pcmData != null?pcmData.length:null));
//
//                if (pcmData == null || pcmData.length == 0){
//                    continue;
//                }
//
//                // === 填入编码器 ===
//                int inIndex = audioEncoder.dequeueInputBuffer(10000);
//                if (inIndex >= 0) {
//                    ByteBuffer inBuffer = audioEncoder.getInputBuffer(inIndex);
//                    inBuffer.clear();
//                    inBuffer.put(pcmData);
//                    audioEncoder.queueInputBuffer(inIndex, 0, pcmData.length, System.nanoTime() / 1000, 0);
//                }
//
//                // === 取出编码后的 AAC 数据 ===
//                int outIndex;
//                while ((outIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 10000)) >= 0) {
//                    ByteBuffer outBuffer = audioEncoder.getOutputBuffer(outIndex);
////                    byte[] aacData = new byte[bufferInfo.size];
////                    outBuffer.get(aacData);
//
//                    // 调用 RtmpClient 发送音频
//                    rtmpUSB.sendAudio(outBuffer, bufferInfo);
//                    audioEncoder.releaseOutputBuffer(outIndex, false);
////                    MyLog.e(TAG + "#encodeLoop once===");
//                }
//            }
//        } catch (Throwable e) {
//            MyLog.e(TAG + "#encodeLoop exception:" +e, e);
//            Thread.currentThread().interrupt();
//        }
//    }

    public void stopEncoding() {
        isEncoding = false;
        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }
    }

    // 在 SerialMicPusher 类中添加以下字段（保持状态跨帧）
    private double hpPrevIn = 0.0;
    private double hpPrevOut = 0.0;
    private double dcAccum = 0.0;
    private final double dcAlpha = 0.999; // DC blocker smoothing
    // Notch filter state (biquad) for mains (configure when constructing)
    private double notch_b0=1, notch_b1=0, notch_b2=0, notch_a1=0, notch_a2=0;
    private double notch_x1=0, notch_x2=0, notch_y1=0, notch_y2=0;
    private boolean useNotch = false;

    // helper: init notch for f0 (50 or 60) and Q
    private void initNotch(double sampleRate, double f0, double Q) {
        double w0 = 2.0 * Math.PI * f0 / sampleRate;
        double alpha = Math.sin(w0) / (2.0 * Q);
        double cosw0 = Math.cos(w0);
        double b0 = 1;
        double b1 = -2 * cosw0;
        double b2 = 1;
        double a0 = 1 + alpha;
        double a1 = -2 * cosw0;
        double a2 = 1 - alpha;
        // normalize b coefficients by a0
        notch_b0 = b0 / a0;
        notch_b1 = b1 / a0;
        notch_b2 = b2 / a0;
        notch_a1 = a1 / a0;
        notch_a2 = a2 / a0;
        notch_x1 = notch_x2 = notch_y1 = notch_y2 = 0.0;
        useNotch = true;
    }

    // apply HPF (1st order) and DC blocker and optional notch
    private byte[] processPcmFrame(byte[] pcmBytes) {
        // assume s16le
        int samples = pcmBytes.length / 2;
        short[] s = new short[samples];
        // convert bytes to shorts (little-endian)
        for (int i = 0, j = 0; i < samples; i++, j += 2) {
            int lo = pcmBytes[j] & 0xFF;
            int hi = pcmBytes[j+1]; // signed
            s[i] = (short)((hi << 8) | lo);
        }

        double fs = SAMPLE_RATE; // use your SAMPLE_RATE
        // HPF coefficient for fc = 80 Hz
        double fc = 80.0;
        double dt = 1.0 / fs;
        double RC = 1.0 / (2 * Math.PI * fc);
        double alpha = RC / (RC + dt); // for y[n] = alpha * (y[n-1] + x[n] - x[n-1])

        int prevIn = 0;
        double prevOut = 0.0;

        for (int i = 0; i < samples; i++) {
            int xi = s[i];
            // DC blocking (exponential moving average)
            dcAccum = dcAlpha * dcAccum + (1.0 - dcAlpha) * xi;
            double x = xi - dcAccum;

            // 1st-order high-pass
            double y = alpha * (prevOut + x - prevIn);
            prevIn = (int)x;
            prevOut = y;

            // optional notch filter (biquad)
            if (useNotch) {
                double x0 = y;
                double y0 = notch_b0 * x0 + notch_b1 * notch_x1 + notch_b2 * notch_x2
                        - notch_a1 * notch_y1 - notch_a2 * notch_y2;
                // shift
                notch_x2 = notch_x1;
                notch_x1 = x0;
                notch_y2 = notch_y1;
                notch_y1 = y0;
                y = y0;
            }

            // simple soft clip / gain control to avoid overflow
            if (y > 32767.0) y = 32767.0;
            if (y < -32768.0) y = -32768.0;
            s[i] = (short)Math.round(y);
        }

        // convert back to bytes
        byte[] out = new byte[samples * 2];
        for (int i = 0, j = 0; i < samples; i++, j+=2) {
            short v = s[i];
            out[j] = (byte)(v & 0xFF);
            out[j+1] = (byte)((v >> 8) & 0xFF);
        }
        return out;
    }

}
