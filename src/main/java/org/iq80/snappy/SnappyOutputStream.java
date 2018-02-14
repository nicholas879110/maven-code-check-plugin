//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.io.IOException;
import java.io.OutputStream;

public class SnappyOutputStream extends OutputStream {
    static final byte[] STREAM_HEADER = new byte[]{115, 110, 97, 112, 112, 121, 0};
    static final int MAX_BLOCK_SIZE = 32768;
    private final BufferRecycler recycler;
    private final byte[] buffer;
    private final byte[] outputBuffer;
    private final OutputStream out;
    private final boolean writeChecksums;
    private int position;
    private boolean closed;

    public SnappyOutputStream(OutputStream out) throws IOException {
        this(out, true);
    }

    public static SnappyOutputStream newChecksumFreeBenchmarkOutputStream(OutputStream out) throws IOException {
        return new SnappyOutputStream(out, false);
    }

    private SnappyOutputStream(OutputStream out, boolean writeChecksums) throws IOException {
        this.out = (OutputStream)SnappyInternalUtils.checkNotNull(out, "out is null", new Object[0]);
        this.writeChecksums = writeChecksums;
        this.recycler = BufferRecycler.instance();
        this.buffer = this.recycler.allocOutputBuffer(32768);
        this.outputBuffer = this.recycler.allocEncodingBuffer(Snappy.maxCompressedLength(32768));
        out.write(STREAM_HEADER);
    }

    public void write(int b) throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        } else {
            if (this.position >= 32768) {
                this.flushBuffer();
            }

            this.buffer[this.position++] = (byte)b;
        }
    }

    public void write(byte[] input, int offset, int length) throws IOException {
        SnappyInternalUtils.checkNotNull(input, "input is null", new Object[0]);
        SnappyInternalUtils.checkPositionIndexes(offset, offset + length, input.length);
        if (this.closed) {
            throw new IOException("Stream is closed");
        } else {
            int free = '耀' - this.position;
            if (free >= length) {
                this.copyToBuffer(input, offset, length);
            } else {
                if (this.position > 0) {
                    this.copyToBuffer(input, offset, free);
                    this.flushBuffer();
                    offset += free;
                    length -= free;
                }

                while(length >= 32768) {
                    this.writeCompressed(input, offset, 32768);
                    offset += 32768;
                    length -= 32768;
                }

                this.copyToBuffer(input, offset, length);
            }
        }
    }

    public void flush() throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        } else {
            this.flushBuffer();
            this.out.flush();
        }
    }

    public void close() throws IOException {
        if (!this.closed) {
            try {
                this.flush();
                this.out.close();
            } finally {
                this.closed = true;
                this.recycler.releaseOutputBuffer(this.outputBuffer);
                this.recycler.releaseEncodeBuffer(this.buffer);
            }

        }
    }

    private void copyToBuffer(byte[] input, int offset, int length) {
        System.arraycopy(input, offset, this.buffer, this.position, length);
        this.position += length;
    }

    private void flushBuffer() throws IOException {
        if (this.position > 0) {
            this.writeCompressed(this.buffer, 0, this.position);
            this.position = 0;
        }

    }

    private void writeCompressed(byte[] input, int offset, int length) throws IOException {
        int crc32c = this.writeChecksums ? Crc32C.maskedCrc32c(input, offset, length) : 0;
        int compressed = Snappy.compress(input, offset, length, this.outputBuffer, 0);
        if (compressed >= length - length / 8) {
            this.writeBlock(input, offset, length, false, crc32c);
        } else {
            this.writeBlock(this.outputBuffer, 0, compressed, true, crc32c);
        }

    }

    private void writeBlock(byte[] data, int offset, int length, boolean compressed, int crc32c) throws IOException {
        this.out.write(compressed ? 1 : 0);
        this.out.write(length >>> 8);
        this.out.write(length);
        this.out.write(crc32c >>> 24);
        this.out.write(crc32c >>> 16);
        this.out.write(crc32c >>> 8);
        this.out.write(crc32c);
        this.out.write(data, offset, length);
    }
}
