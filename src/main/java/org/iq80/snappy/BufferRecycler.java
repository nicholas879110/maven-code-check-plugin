//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.lang.ref.SoftReference;

class BufferRecycler {
    private static final int MIN_ENCODING_BUFFER = 4000;
    private static final int MIN_OUTPUT_BUFFER = 8000;
    protected static final ThreadLocal<SoftReference<BufferRecycler>> recyclerRef = new ThreadLocal();
    private byte[] inputBuffer;
    private byte[] outputBuffer;
    private byte[] decodingBuffer;
    private byte[] encodingBuffer;
    private short[] encodingHash;

    BufferRecycler() {
    }

    public static BufferRecycler instance() {
        SoftReference<BufferRecycler> ref = (SoftReference)recyclerRef.get();
        BufferRecycler bufferRecycler;
        if (ref == null) {
            bufferRecycler = null;
        } else {
            bufferRecycler = (BufferRecycler)ref.get();
        }

        if (bufferRecycler == null) {
            bufferRecycler = new BufferRecycler();
            recyclerRef.set(new SoftReference(bufferRecycler));
        }

        return bufferRecycler;
    }

    public byte[] allocEncodingBuffer(int minSize) {
        byte[] buf = this.encodingBuffer;
        if (buf != null && buf.length >= minSize) {
            this.encodingBuffer = null;
        } else {
            buf = new byte[Math.max(minSize, 4000)];
        }

        return buf;
    }

    public void releaseEncodeBuffer(byte[] buffer) {
        if (this.encodingBuffer == null || buffer.length > this.encodingBuffer.length) {
            this.encodingBuffer = buffer;
        }

    }

    public byte[] allocOutputBuffer(int minSize) {
        byte[] buf = this.outputBuffer;
        if (buf != null && buf.length >= minSize) {
            this.outputBuffer = null;
        } else {
            buf = new byte[Math.max(minSize, 8000)];
        }

        return buf;
    }

    public void releaseOutputBuffer(byte[] buffer) {
        if (this.outputBuffer == null || buffer != null && buffer.length > this.outputBuffer.length) {
            this.outputBuffer = buffer;
        }

    }

    public short[] allocEncodingHash(int suggestedSize) {
        short[] buf = this.encodingHash;
        if (buf != null && buf.length >= suggestedSize) {
            this.encodingHash = null;
        } else {
            buf = new short[suggestedSize];
        }

        return buf;
    }

    public void releaseEncodingHash(short[] buffer) {
        if (this.encodingHash == null || buffer != null && buffer.length > this.encodingHash.length) {
            this.encodingHash = buffer;
        }

    }

    public byte[] allocInputBuffer(int minSize) {
        byte[] buf = this.inputBuffer;
        if (buf != null && buf.length >= minSize) {
            this.inputBuffer = null;
        } else {
            buf = new byte[Math.max(minSize, 8000)];
        }

        return buf;
    }

    public void releaseInputBuffer(byte[] buffer) {
        if (this.inputBuffer == null || buffer != null && buffer.length > this.inputBuffer.length) {
            this.inputBuffer = buffer;
        }

    }

    public byte[] allocDecodeBuffer(int size) {
        byte[] buf = this.decodingBuffer;
        if (buf != null && buf.length >= size) {
            this.decodingBuffer = null;
        } else {
            buf = new byte[size];
        }

        return buf;
    }

    public void releaseDecodeBuffer(byte[] buffer) {
        if (this.decodingBuffer == null || buffer != null && buffer.length > this.decodingBuffer.length) {
            this.decodingBuffer = buffer;
        }

    }
}
