package dev.alejandrorosas.streamlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;

import com.base.MyLog;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class SerialEarPlayer {

    public static final String TAG = "[SerialEarPlayer]" ;

    private final UsbSerialPort port;
    private Thread playThread;
    @SuppressLint("NewApi")
    private final ExecutorService workThreadExecutor = Executors.newWorkStealingPool();

    private final AtomicBoolean earWriterRunning = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);


    private int sampleRate = 16000;
    private int channels = 1;
    private int bitsPerSample = 16;
    // 控制参数
    private volatile float gainDb = 0.0f;
    private  PortListener mPortListener ;
    private volatile boolean applyLimiter = true;

    private final BlockingDeque<byte[]> earQueue = new LinkedBlockingDeque<>(1000); // capacity 可调
    private static final int WRITE_TIMEOUT_MS = 1000;
    public static SerialEarPlayer sSerialEarPlayer;

    private final int FRAME_BYTES = 512;
    private final int TAIL_SILENCE_FRAMES = 3;
    private final int POLL_TIMEOUT_MS = 200;
    private final int DROP_THRESHOLD = 400;


    public static SerialEarPlayer testPlay(Context context){
        SerialEarPlayer serialEarPlayer = new SerialEarPlayer(UsbDeviceManager.getInstance().getMicPort());

        serialEarPlayer.startEarWriter(serialEarPlayer.port);

//        File wav = new File("/sdcard/Download", "tts_capture.wav");
//        serialEarPlayer.setGainDb(20.0f);
//        serialEarPlayer.startPlaybackFromWav(wav);

        sSerialEarPlayer = serialEarPlayer;
        return serialEarPlayer;
    }

    public static void testStopPlay(Context context){
        if (sSerialEarPlayer != null){
            sSerialEarPlayer.stopEarWriterGraceful(true);
            sSerialEarPlayer.stop();
            sSerialEarPlayer = null;
        }
    }




    public SerialEarPlayer(UsbSerialPort port) {
        this.port = port;
    }

    public void setGainDb(float db) {
        this.gainDb = db;
    }
    public void setPortListener( PortListener listener) {
        this.mPortListener = listener;
    }

    public void setApplyLimiter(boolean apply) {
        this.applyLimiter = apply;
    }

    public void startPlaybackFromWav(File wavFile) {
        stop(); // stop previous
        running.set(true);
        playThread = new Thread(() -> {
            try {
                playWavFileRealtime(wavFile);
            } catch (Exception e) {
                MyLog.e(TAG + "playback error", e);
            } finally {
                running.set(false);
            }
        }, "SerialAudioPlayer-Play");
        playThread.start();
    }

    public void stop() {
        running.set(false);
        if (playThread != null) {
            playThread.interrupt();
            try { playThread.join(500); } catch (InterruptedException ignored) {}
            playThread = null;
        }
        earQueue.clear();
    }

    // ----------------- internal -----------------

    private void playWavFileRealtime(File wavFile) throws IOException {
        FileInputStream fis = new FileInputStream(wavFile);
        DataInputStream dis = new DataInputStream(fis);

        // parse minimal WAV header
        byte[] header = new byte[44];
        if (dis.read(header) != 44) {
            dis.close();
            throw new IOException("Invalid WAV header (too short)");
        }
        // check "RIFF" and "WAVE"
        if (!(header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E')) {
            dis.close();
            throw new IOException("Not a WAV file");
        }
        // read channels (offset 22, little endian short)
        channels = readLEShort(header, 22);
        bitsPerSample = readLEShort(header, 34);
        sampleRate = readLEInt(header, 24);

       MyLog.e(TAG + "wav format: sr=" + sampleRate + " ch=" + channels + " bits=" + bitsPerSample);

        if (bitsPerSample != 16) {
            MyLog.e(TAG + "Expected 16-bit WAV. behavior undefined.");
        }

        playPcmStreamRealtime(dis, sampleRate, channels, bitsPerSample);

        dis.close();
    }

    private void playPcmStreamRealtime(DataInputStream dis,
                                       int sampleRate,
                                       int channels,
                                       int bitsPerSample) throws IOException {
        final int bytesPerSample = bitsPerSample / 8;
        final int frameSize = 320; // 320 bytes as you requested (=160 samples @16k mono)
        final int bytesPerMs = (sampleRate * channels * bytesPerSample) / 1000;
        final long chunkDurationNs = (long) frameSize * 1_000_000_000L / (sampleRate * channels * bytesPerSample); // ns

        byte[] readBuf = new byte[frameSize];
        long nextSendNs = System.nanoTime();

        while (running.get()) {
            int read = dis.read(readBuf);
            if (read <= 0) break;

            byte[] chunk;
            if (read == frameSize) {
                chunk = readBuf.clone(); // safe copy
            } else {
                // final partial block - pad with zeros
                chunk = new byte[read];
                System.arraycopy(readBuf, 0, chunk, 0, read);
            }

            // apply gain and optional limiter
            applyGainAndLimiterPcm16leInplace(chunk, gainDb, applyLimiter);

            // write to serial port (handle partial writes)
            enqueueChunkToEarQueue(chunk);

            // precise pacing using nanoTime
            nextSendNs += chunkDurationNs;
            long sleepNs = nextSendNs - System.nanoTime();
            if (sleepNs > 2_000_000L) { // >2ms
                try {
                    long ms = sleepNs / 1_000_000L;
                    int ns = (int) (sleepNs % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else if (sleepNs > 0) {
                // short wait via parkNanos
                LockSupport.parkNanos(sleepNs);
            } else {
                // we're late; continue immediately (could log)
                // optionally can catch up by not sleeping
            }
        }
    }

//    /** Write chunk to UsbSerialPort safely, handling partial writes and timeouts. */
//    private void sendChunkToPort(byte[] chunk) {
//        if (port == null) return;
//        int offset = 0;
//        int toWrite = chunk.length;
//        final int WRITE_TIMEOUT_MS = 1000;
//        try {
//            while (offset < toWrite) {
//                int wrote = port.write(chunk, offset, toWrite - offset, WRITE_TIMEOUT_MS);
//                if (wrote <= 0) {
//                    // nothing written - break to avoid infinite loop
//                    break;
//                }
//                offset += wrote;
//            }
//        } catch (IOException e) {
//            MyLog.e(TAG + "serial write error", e);
//            // consider stopping if port error
//            running.set(false);
//        }
//    }

    public void enqueueChunkToEarQueue(byte[] chunk) {
        if (chunk == null || chunk.length == 0) return;
//        MyLog.e(TAG + "enqueueChunkToEarQueue earQueue:" + earQueue.size());
        boolean ok = earQueue.offerLast(chunk);
        if (!ok) {
            // 丢弃最旧
            earQueue.pollFirst();
            earQueue.offerLast(chunk);
        }
    }

    private void startEarWriter(final UsbSerialPort port) {
        MyLog.e(TAG + "ear writer port:" + port);
        if (earWriterRunning.get()) return;
        earWriterRunning.set(true);
        workThreadExecutor.submit(() -> {
            final byte[] silenceFrame = new byte[FRAME_BYTES];
            final long bytesPerSecond = (long) sampleRate * channels * (bitsPerSample / 8);
            final long frameDurationNs = (FRAME_BYTES * 1_000_000_000L) / bytesPerSecond;
            try {
                while (earWriterRunning.get() && !Thread.currentThread().isInterrupted()) {
                    int qsz = earQueue.size();
//                    MyLog.e(TAG + "ear writer earQueue:" + qsz);

                    if (qsz > DROP_THRESHOLD) {
                        int toDrop = Math.min(qsz - DROP_THRESHOLD, qsz / 4);
                        for (int i = 0; i < toDrop; i++) earQueue.pollFirst();
                        MyLog.e(TAG + "dropped " + toDrop + " frames to catch up, newQueue=" + earQueue.size());
                    }

                    byte[] chunk = null;

                    try {
                        chunk = earQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if (chunk == null) {
                        continue;
                    }

                    if (chunk.length != FRAME_BYTES) {
                        if (chunk.length < FRAME_BYTES) {
                            byte[] padded = new byte[FRAME_BYTES];
                            System.arraycopy(chunk, 0, padded, 0, chunk.length);
                            chunk = padded;
                        } else {
                            byte[] trimmed = new byte[FRAME_BYTES];
                            System.arraycopy(chunk, 0, trimmed, 0, FRAME_BYTES);
                            chunk = trimmed;
                        }
                    }

                    long t0 = System.nanoTime();
                    try {
                        port.write(chunk, WRITE_TIMEOUT_MS);

                        if (mPortListener != null){
                            mPortListener.onData(chunk);
                        }


                    } catch (IOException e) {
                        MyLog.e(TAG + "ear writer write error", e);
                        try {
                            SystemClock.sleep(20);
                            port.write(chunk, WRITE_TIMEOUT_MS);
                        } catch (Exception retryEx) {
                            MyLog.e(TAG + "ear writer retry failed", retryEx);
                            break;
                        }
                    }
                    long tookMs = (System.nanoTime() - t0) / 1_000_000L;


                    long sleepNs = frameDurationNs - (System.nanoTime() - t0);

                    if (sleepNs > 2_000_000L) {//2ms
                        try {
                            long ms = sleepNs / 1_000_000L;
                            int ns = (int) (sleepNs % 1_000_000L);
                            Thread.sleep(ms, ns);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else if (sleepNs > 0) {
                        LockSupport.parkNanos(sleepNs);
                    }

//                    byte[] chunk = earQueue.take();
//                    if (chunk == null) continue;
//
//                    try {
//                        port.write(chunk, WRITE_TIMEOUT_MS);
//                        // port.write(chunk, chunk.length, WRITE_TIMEOUT_MS);
//                    } catch (IOException e) {
//                        MyLog.e(TAG + "ear writer write error", e);
//                        try {
//                            SystemClock.sleep(50);
//                            port.write(chunk, WRITE_TIMEOUT_MS);
//                        } catch (Exception retryEx) {
//                            MyLog.e(TAG + "ear writer retry failed", retryEx);
//                            // 如果端口不可用，可能需要通知上层重连并清空队列
//                        }
//                    }
                }

                for (int i = 0; i < TAIL_SILENCE_FRAMES; i++) {
                    try {
                        port.write(silenceFrame, WRITE_TIMEOUT_MS);
                    } catch (Exception e) {
                        // ignore
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            } catch (Throwable ie) {
                MyLog.e(TAG + "startEarWriter exception:"  + ie);
            } finally {
                earWriterRunning.set(false);
                MyLog.e(TAG + "earWriterThread exit");
            }
        });
    }

    public void stopEarWriterGraceful(boolean drain) {
        MyLog.e(TAG + "stopEarWriterGraceful drain=" + drain);
        if (!earWriterRunning.get()) {
            earQueue.clear();
            return;
        }
        if (!drain) {
            earQueue.clear();
        }
        final long start = System.currentTimeMillis();
        final long waitMs = 5000; // wait up to 5s for drain
        new Thread(() -> {
            try {
                while (earWriterRunning.get() && (!earQueue.isEmpty())) {
                    Thread.sleep(20);
                    if (System.currentTimeMillis() - start > waitMs) break;
                }
            } catch (InterruptedException ignored) {
            }
            earWriterRunning.set(false);
        }, "EarWriterStopMonitor").start();

        long waitForExitStart = System.currentTimeMillis();
        while (earWriterRunning.get() && System.currentTimeMillis() - waitForExitStart < 1500) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    public int getEarQueueSize() {
        return earQueue.size();
    }

    public boolean isEarWriterRunning() {
        return earWriterRunning.get();
    }


    // ----------------- PCM processing -----------------

    /** Apply gain (dB) to PCM16LE data in-place; optional simple limiter prevents clipping. */
    private static void applyGainAndLimiterPcm16leInplace(byte[] data, float gainDb, boolean limiter) {
        if (data == null || data.length < 2) return;
        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        if (Math.abs(gain - 1.0f) < 1e-6 && !limiter) return;

        int maxAbs = 0;
        for (int i = 0; i + 1 < data.length; i += 2) {
            int low = data[i] & 0xff;
            int high = data[i + 1];
            int sample = (high << 8) | low;
            int amplified = Math.round(sample * gain);
            if (amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
            if (amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;
            if (Math.abs(amplified) > maxAbs) maxAbs = Math.abs(amplified);
            data[i] = (byte) (amplified & 0xff);
            data[i + 1] = (byte) ((amplified >> 8) & 0xff);
        }

        if (limiter && maxAbs >= Short.MAX_VALUE) {
            float scale = (Short.MAX_VALUE * 0.98f) / (float) maxAbs;
            for (int i = 0; i + 1 < data.length; i += 2) {
                int low = data[i] & 0xff;
                int high = data[i + 1];
                int sample = (high << 8) | low;
                int scaled = Math.round(sample * scale);
                if (scaled > Short.MAX_VALUE) scaled = Short.MAX_VALUE;
                if (scaled < Short.MIN_VALUE) scaled = Short.MIN_VALUE;
                data[i] = (byte) (scaled & 0xff);
                data[i + 1] = (byte) ((scaled >> 8) & 0xff);
            }
        }
    }

    private static int readLEInt(byte[] b, int offset) {
        return ((b[offset] & 0xff)) |
                ((b[offset + 1] & 0xff) << 8) |
                ((b[offset + 2] & 0xff) << 16) |
                ((b[offset + 3] & 0xff) << 24);
    }
    private static int readLEShort(byte[] b, int offset) {
        return ((b[offset] & 0xff)) | ((b[offset + 1] & 0xff) << 8);
    }

    public interface PortListener {
        void onData(byte[] frame);

    }
}
