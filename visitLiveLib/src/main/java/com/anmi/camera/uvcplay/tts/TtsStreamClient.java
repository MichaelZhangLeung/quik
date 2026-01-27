package com.anmi.camera.uvcplay.tts;

import android.text.TextUtils;

import com.anmi.camera.uvcplay.utils.PcmFileWriter;
import com.base.MyLog;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.alejandrorosas.streamlib.SerialEarPlayer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class TtsStreamClient implements SerialEarPlayer.PortListener {

    private static final String TAG = "TtsStreamClient";
    private final String wsUrl;
    private final SerialEarPlayer serialPlayer;
    private final OkHttpClient httpClient;
    private WebSocket webSocket;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean closedByUser = new AtomicBoolean(false);

    private final ByteBuffer recvBuffer;
    private final int FRAME_SIZE;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService testExecutor =  Executors.newSingleThreadExecutor();
    private int reconnectAttempt = 0;
    private final int maxReconnectDelaySec = 60;

    private PcmFileWriter pcmWriter;

    private final java.util.concurrent.BlockingQueue<byte[]> incomingFramesQueue = new java.util.concurrent.LinkedBlockingQueue<>(3000);
    private final ScheduledExecutorService pacerScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean pacerRunning = false;
    private final int FRAME_BYTES = 512; // 与服务器包大小一致
    private final int SAMPLE_RATE = 16000;
    private final int CHANNELS = 1;
    private final int BYTES_PER_SAMPLE = 2;
    private final long FRAME_DURATION_MS = (FRAME_BYTES * 1000L) / (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE); // ~16 ms

    private volatile boolean isDone = false;

    @Override
    public void onData(byte[] frame) {
//        if (pcmWriter != null) {
//            testExecutor.submit(() -> {
//                try {
//                    if (pcmWriter != null) pcmWriter.write(frame);
//                } catch (IOException e) {
//                    logError("pcmWriter write exception:" + e, e);
//                }
//            });
//        }
    }

    public interface Listener {
        void onOpen();
        void onClose(int code, String reason);
        void onError(Throwable t);
        void onInfo(String info);
    }
    private Listener listener;

    public TtsStreamClient(String wsUrl, SerialEarPlayer serialPlayer, int frameSize, int recvBufferCapacity) {
        if (serialPlayer == null) throw new IllegalArgumentException("serialPlayer required");
        if (frameSize <= 0 || recvBufferCapacity < frameSize) throw new IllegalArgumentException("invalid sizes");

        this.wsUrl = wsUrl;
        this.serialPlayer = serialPlayer;
        this.FRAME_SIZE = frameSize;
        this.recvBuffer = ByteBuffer.allocate(recvBufferCapacity);

        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.serialPlayer.setPortListener(this);

        File testWav = new File("/sdcard/Download", "tts_capture.wav");
        pcmWriter = new PcmFileWriter(testWav, 16000, 1, 16);
        try {
            pcmWriter.open();
        } catch (IOException e) {
            logError("pcmWriter open exception:" + e, e);
        }
    }

    public TtsStreamClient(String wsUrl, SerialEarPlayer serialPlayer, int frameSize) {
        this(wsUrl, serialPlayer, frameSize, 64 * 1024);
    }

    public void setListener(Listener l) { this.listener = l; }

    public void connect() {
        if (connecting.get() || connected.get()) {
            logInfo("already connecting/connected");
            return;
        }
        closedByUser.set(false);
        doConnectWithBackoff(0);
    }

    private void startPacer() {
        if (pacerRunning) return;
        pacerRunning = true;
        pacerScheduler.scheduleAtFixedRate(() -> {
            try {
                MyLog.e(TAG + " #startPacer incomingFramesQueue:" + incomingFramesQueue.size());
                byte[] frame = incomingFramesQueue.poll(); // non-blocking

                if (frame == null) {
                    if (isDone) {
                        finalizeAfterDrain();
                    }
                    return;
                }

                if (frame.length != FRAME_BYTES) {
                    if (frame.length < FRAME_BYTES) {
                        byte[] padded = new byte[FRAME_BYTES];
                        System.arraycopy(frame, 0, padded, 0, frame.length);
                        frame = padded;
                    } else {
                        byte[] trimmed = new byte[FRAME_BYTES];
                        System.arraycopy(frame, 0, trimmed, 0, FRAME_BYTES);
                        frame = trimmed;
                    }
                }

                serialPlayer.enqueueChunkToEarQueue(frame);

//                if (pcmWriter != null) {
//                    byte[] finalFrame = frame;
//                    testExecutor.submit(() -> {
//                        try {
//                            if (pcmWriter != null) pcmWriter.write(finalFrame);
//                        } catch (IOException e) {
//                            logError("pcmWriter write exception:" + e, e);
//                        }
//                    });
//                }
            } catch (Throwable t) {
                MyLog.e(TAG + " pacer error", t);
            }
        }, 0, Math.max(1, FRAME_DURATION_MS), TimeUnit.MILLISECONDS);
    }

    private void stopPacer() {
        pacerRunning = false;
        try {
            pacerScheduler.shutdownNow();
        } catch (Exception e) {
            MyLog.e(TAG + " stopPacer shutdownNow error", e);
        }
    }

    public void disconnect() {
        closedByUser.set(true);
        reconnectAttempt = 0;
        if (webSocket != null) {
            try {
                webSocket.close(1000, "client disconnect");
            } catch (Exception ignored) {}
            webSocket = null;
        }
        connected.set(false);
        connecting.set(false);

        stopPacer();
        if (pcmWriter != null) {
            pcmWriter.close(); // 会写入 WAV header 并 close 文件
            pcmWriter = null;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void speak(String text) {
        if (!isConnected()) {
            logInfo("speak: websocket not connected");
            return;
        }
        try {
            JSONObject j = new JSONObject();
            j.put("type", "speak");
            j.put("text", text);
            webSocket.send(j.toString());
        } catch (Exception e) {
            logError("speak send error", e);
        }
    }





    private final AtomicBoolean finalizeScheduled = new AtomicBoolean(false);
    private void finalizeAfterDrain() {
        if (!finalizeScheduled.compareAndSet(false, true)) return; // only schedule once
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                long start = System.currentTimeMillis();
                long timeoutMs = 60_000;
                while (true) {
                    int q = serialPlayer.getEarQueueSize();
                    if (q <= 0) break;
                    if (System.currentTimeMillis() - start > timeoutMs) {
                        MyLog.e(TAG + " finalizeAfterDrain timeout waiting for earQueue to drain, q=" + q);
                        break;
                    }
                    Thread.sleep(20);
                }
                serialPlayer.stopEarWriterGraceful(true);
                stopPacer();
                serialPlayer.stop();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                finalizeScheduled.set(false);
            }
        });
    }

    private void doConnectWithBackoff(long delaySec) {
        scheduler.schedule(() -> {
            if (closedByUser.get()) {
                logInfo("not connecting because closedByUser");
                return;
            }
            connecting.set(true);
            try {
                Request req = new Request.Builder().url(wsUrl).build();
                webSocket = httpClient.newWebSocket(req, new WsListener());
                logInfo("websocket connect requested");
            } catch (Exception e) {
                connecting.set(false);
                scheduleReconnect();
                logError("websocket connect exception", e);
            }
        }, delaySec, TimeUnit.SECONDS);
    }

    private void scheduleReconnect() {
        if (closedByUser.get()) return;
        reconnectAttempt++;
        long delay = Math.min((1 << Math.min(reconnectAttempt, 10)), maxReconnectDelaySec); // exponential but capped
        logInfo("scheduling reconnect in " + delay + "s (attempt " + reconnectAttempt + ")");
        doConnectWithBackoff(delay);
    }

    private class WsListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket ws, Response response) {
            connecting.set(false);
            connected.set(true);
            reconnectAttempt = 0;
            logInfo("ws onOpen");
            startPacer();
            if (listener != null) listener.onOpen();
        }

        @Override
        public void onMessage(WebSocket ws, String text) {
            logInfo("ws text message: " + text);
            if (text.startsWith("{")) {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    String event = jsonObject.getString("event");
                    if (TextUtils.equals(event, "done")){
                        isDone = true;
                        finalizeAfterDrain();
                    }
                } catch (Throwable e) {
                    logError("ws text message err: " + e, e);
                }
            }

            if (listener != null) {
                listener.onInfo(text);
            }
        }

        @Override
        public void onMessage(WebSocket ws, ByteString bytes) {
            try {
                MyLog.e(TAG + "#onMessage bytes:" + bytes.size());
                handleIncomingPcmChunk(bytes.toByteArray());

                testExecutor.submit(() -> {
                    try {
                        if (pcmWriter != null) pcmWriter.write(bytes.toByteArray());
                    } catch (IOException e) {
                        logError("pcmWriter write exception:" + e, e);
                    }
                });
            } catch (Exception e) {
                logError("handleIncomingPcmChunk error", e);
            }
        }

        @Override
        public void onClosing(WebSocket ws, int code, String reason) {
            logInfo("ws onClosing code=" + code + " reason=" + reason);
            connected.set(false);
            connecting.set(false);
            if (listener != null) listener.onClose(code, reason);
        }

        @Override
        public void onClosed(WebSocket ws, int code, String reason) {
            logInfo("ws onClosed code=" + code + " reason=" + reason);
            connected.set(false);
            connecting.set(false);
            if (!closedByUser.get()) {
                scheduleReconnect();
            }

            stopPacer();

            if (pcmWriter != null) {
                pcmWriter.close(); // 会写入 WAV header 并 close 文件
                pcmWriter = null;
            }

            if (listener != null) listener.onClose(code, reason);
        }

        @Override
        public void onFailure(WebSocket ws, Throwable t, Response response) {
            logError("ws onFailure", t);
            connected.set(false);
            connecting.set(false);

            stopPacer();

            if (!closedByUser.get()) scheduleReconnect();
            if (listener != null) listener.onError(t);
        }
    }


    private void handleIncomingPcmChunk(byte[] incoming) {
        if (incoming == null || incoming.length == 0) return;

        synchronized (recvBuffer) {
            if (recvBuffer.remaining() < incoming.length) {
                recvBuffer.flip();
                while (recvBuffer.remaining() >= FRAME_SIZE) {
                    byte[] frame = new byte[FRAME_SIZE];
                    recvBuffer.get(frame);
                    enqueueToSerial(frame);
                }
                int left = recvBuffer.remaining();
                if (left > 0) {
                    byte[] leftover = new byte[left];
                    recvBuffer.get(leftover);
                    recvBuffer.clear();
                    recvBuffer.put(leftover);
                } else {
                    recvBuffer.clear();
                }
            }
            if (recvBuffer.remaining() < incoming.length) {
                logInfo("recvBuffer overflow, clearing buffer to accept incoming chunk");
                recvBuffer.clear();
            }
            recvBuffer.put(incoming);

            recvBuffer.flip();
            while (recvBuffer.remaining() >= FRAME_SIZE) {
                byte[] frame = new byte[FRAME_SIZE];
                recvBuffer.get(frame);
                enqueueToSerial(frame);
            }
            int remain = recvBuffer.remaining();
            if (remain > 0) {
                byte[] left = new byte[remain];
                recvBuffer.get(left);
                recvBuffer.clear();
                recvBuffer.put(left);
            } else {
                recvBuffer.clear();
            }
        }
    }

    private void enqueueToSerial(byte[] frame) {
        try {
            boolean offered = incomingFramesQueue.offer(frame);
            if (!offered) {
                incomingFramesQueue.poll();
                incomingFramesQueue.offer(frame);
            }
        } catch (Exception e) {
            MyLog.e(TAG +"enqueueToSerial fallback: " + e);
        }
    }

    private void logInfo(String s) {
        MyLog.e(TAG + s);
        if (listener != null) listener.onInfo(s);
    }

    private void logError(String s, Throwable t) {
       MyLog.e(TAG + s, t);
        if (listener != null) listener.onError(t);
    }
}
