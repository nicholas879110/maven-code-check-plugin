//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class SnappyInputStream extends InputStream {
    private final BufferRecycler recycler;
    private final byte[] input;
    private final byte[] uncompressed;
    private final byte[] header;
    private final InputStream in;
    private final boolean verifyChecksums;
    private byte[] buffer;
    private int valid;
    private int position;
    private boolean closed;
    private boolean eof;

    public SnappyInputStream(InputStream in) throws IOException {
        this(in, true);
    }

    public SnappyInputStream(InputStream in, boolean verifyChecksums) throws IOException {
        this.header = new byte[7];
        this.in = in;
        this.verifyChecksums = verifyChecksums;
        this.recycler = BufferRecycler.instance();
        this.input = this.recycler.allocInputBuffer(32768);
        this.uncompressed = this.recycler.allocDecodeBuffer(32768);

        int size;
        for(int offset = 0; offset < this.header.length; offset += size) {
            size = in.read(this.header, offset, this.header.length - offset);
            if (size == -1) {
                throw new EOFException("encountered EOF while reading stream header");
            }
        }

        if (!Arrays.equals(this.header, SnappyOutputStream.STREAM_HEADER)) {
            throw new IOException("invalid stream header");
        }
    }

    public int read() throws IOException {
        if (this.closed) {
            return -1;
        } else {
            return !this.ensureBuffer() ? -1 : this.buffer[this.position++] & 255;
        }
    }

    public int read(byte[] output, int offset, int length) throws IOException {
        SnappyInternalUtils.checkNotNull(output, "output is null", new Object[0]);
        SnappyInternalUtils.checkPositionIndexes(offset, offset + length, output.length);
        if (this.closed) {
            throw new IOException("Stream is closed");
        } else if (length == 0) {
            return 0;
        } else if (!this.ensureBuffer()) {
            return -1;
        } else {
            int size = Math.min(length, this.available());
            System.arraycopy(this.buffer, this.position, output, offset, size);
            this.position += size;
            return size;
        }
    }

    public int available() throws IOException {
        return this.closed ? 0 : this.valid - this.position;
    }

    public void close() throws IOException {
        try {
            this.in.close();
        } finally {
            if (!this.closed) {
                this.closed = true;
                this.recycler.releaseInputBuffer(this.input);
                this.recycler.releaseDecodeBuffer(this.uncompressed);
            }

        }

    }

    private boolean ensureBuffer() throws IOException {
        if (this.available() > 0) {
            return true;
        } else if (this.eof) {
            return false;
        } else if (!this.readBlockHeader()) {
            this.eof = true;
            return false;
        } else {
            boolean compressed = this.getHeaderCompressedFlag();
            int length = this.getHeaderLength();
            this.readInput(length);
            this.handleInput(length, compressed);
            return true;
        }
    }

    private void handleInput(int length, boolean compressed) throws IOException {
        if (compressed) {
            this.buffer = this.uncompressed;

            try {
                this.valid = Snappy.uncompress(this.input, 0, length, this.uncompressed, 0);
            } catch (CorruptionException var5) {
                throw new IOException("Corrupt input", var5);
            }
        } else {
            this.buffer = this.input;
            this.valid = length;
        }

        if (this.verifyChecksums) {
            int expectedCrc32c = this.getCrc32c();
            int actualCrc32c = Crc32C.maskedCrc32c(this.buffer, 0, this.valid);
            if (expectedCrc32c != actualCrc32c) {
                throw new IOException("Corrupt input: invalid checksum");
            }
        }

        this.position = 0;
    }

    private void readInput(int length) throws IOException {
        int size;
        for(int offset = 0; offset < length; offset += size) {
            size = this.in.read(this.input, offset, length - offset);
            if (size == -1) {
                throw new EOFException("encountered EOF while reading block data");
            }
        }

    }

    private boolean readBlockHeader() throws IOException {
        int size;
        do {
            for(int offset = 0; offset < this.header.length; offset += size) {
                size = this.in.read(this.header, offset, this.header.length - offset);
                if (size == -1) {
                    if (offset == 0) {
                        return false;
                    }

                    throw new EOFException("encountered EOF while reading block header");
                }
            }
        } while(Arrays.equals(this.header, SnappyOutputStream.STREAM_HEADER));

        return true;
    }

    private boolean getHeaderCompressedFlag() throws IOException {
        int x = this.header[0] & 255;
        switch(x) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new IOException(String.format("invalid compressed flag in header: 0x%02x", x));
        }
    }

    private int getHeaderLength() throws IOException {
        int a = this.header[1] & 255;
        int b = this.header[2] & 255;
        int length = a << 8 | b;
        if (length > 0 && length <= 32768) {
            return length;
        } else {
            throw new IOException("invalid block size in header: " + length);
        }
    }

    private int getCrc32c() throws IOException {
        return (this.header[3] & 255) << 24 | (this.header[4] & 255) << 16 | (this.header[5] & 255) << 8 | this.header[6] & 255;
    }
}
