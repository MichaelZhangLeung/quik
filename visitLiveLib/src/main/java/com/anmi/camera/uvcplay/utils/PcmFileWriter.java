package com.anmi.camera.uvcplay.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * PcmFileWriter
 *
 * 用法:
 *   PcmFileWriter w = new PcmFileWriter(file, 16000, 1, 16);
 *   w.open();        // 开始写入（会创建文件并占位 wav header）
 *   w.write(bytes);  // 多次调用写 PCM bytes
 *   w.close();       // 完成并写入正确的 wav header
 *
 * 也可以只用 writeRaw(...) -> 直接生成 raw 文件（无 header）。
 */
public class PcmFileWriter {
    private final File file;
    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;

    private RandomAccessFile raf;
    private long dataSize = 0;

    public PcmFileWriter(File file, int sampleRate, int channels, int bitsPerSample) {
        this.file = file;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
    }

    /** Open file and write placeholder WAV header (44 bytes) */
    public void open() throws IOException {
        raf = new RandomAccessFile(file, "rw");
        raf.setLength(0);
        // write 44 zero bytes as placeholder (will overwrite in close)
        byte[] header = new byte[44];
        raf.write(header);
        dataSize = 0;
    }

    /** Write raw PCM bytes (append). */
    public synchronized void write(byte[] pcm) throws IOException {
        if (raf == null) throw new IOException("Not opened");
        raf.write(pcm);
        dataSize += pcm.length;
    }

    /** Close and write WAV header (overwrite beginning). */
    public synchronized void close() {
        if (raf == null) return;
        try {
            // finalize header
            raf.seek(0);
            writeWavHeader(raf, dataSize, sampleRate, channels, bitsPerSample);
        } catch (IOException e) {
            // ignore header write failure
        } finally {
            try { raf.close(); } catch (IOException ignored) {}
            raf = null;
        }
    }

    /** Convenience: write raw bytes directly to a file (no wav header). */
    public static void writeRaw(File out, byte[] bytes) throws IOException {
        RandomAccessFile r = new RandomAccessFile(out, "rw");
        r.setLength(0);
        r.write(bytes);
        r.close();
    }

    // --------- helper to write WAV header (little-endian) -----------
    private static void writeWavHeader(RandomAccessFile out, long pcmDataSize,
                                       int sampleRate, int channels, int bitsPerSample) throws IOException {
        long byteRate = (sampleRate * channels * bitsPerSample) / 8;
        int blockAlign = (channels * bitsPerSample) / 8;
        long subChunk2Size = pcmDataSize;
        long chunkSize = 36 + subChunk2Size;

        // "RIFF" chunk descriptor
        out.writeBytes("RIFF");
        writeIntLE(out, (int) chunkSize);
        out.writeBytes("WAVE");

        // fmt subchunk
        out.writeBytes("fmt ");
        writeIntLE(out, 16); // Subchunk1Size for PCM
        writeShortLE(out, (short) 1); // AudioFormat = 1 (PCM)
        writeShortLE(out, (short) channels); // NumChannels
        writeIntLE(out, sampleRate); // SampleRate
        writeIntLE(out, (int) byteRate); // ByteRate
        writeShortLE(out, (short) blockAlign); // BlockAlign
        writeShortLE(out, (short) bitsPerSample); // BitsPerSample

        // data subchunk
        out.writeBytes("data");
        writeIntLE(out, (int) subChunk2Size);
    }

    private static void writeIntLE(RandomAccessFile out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }
    private static void writeShortLE(RandomAccessFile out, short value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }
}
