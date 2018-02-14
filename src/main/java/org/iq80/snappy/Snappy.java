//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.util.Arrays;

public final class Snappy {
    static final int LITERAL = 0;
    static final int COPY_1_BYTE_OFFSET = 1;
    static final int COPY_2_BYTE_OFFSET = 2;
    static final int COPY_4_BYTE_OFFSET = 3;

    private Snappy() {
    }

    public static int getUncompressedLength(byte[] compressed, int compressedOffset) throws CorruptionException {
        return SnappyDecompressor.getUncompressedLength(compressed, compressedOffset);
    }

    public static byte[] uncompress(byte[] compressed, int compressedOffset, int compressedSize) throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize);
    }

    public static int uncompress(byte[] compressed, int compressedOffset, int compressedSize, byte[] uncompressed, int uncompressedOffset) throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize, uncompressed, uncompressedOffset);
    }

    public static int maxCompressedLength(int sourceLength) {
        return SnappyCompressor.maxCompressedLength(sourceLength);
    }

    public static int compress(byte[] uncompressed, int uncompressedOffset, int uncompressedLength, byte[] compressed, int compressedOffset) {
        return SnappyCompressor.compress(uncompressed, uncompressedOffset, uncompressedLength, compressed, compressedOffset);
    }

    public static byte[] compress(byte[] data) {
        byte[] compressedOut = new byte[maxCompressedLength(data.length)];
        int compressedSize = compress(data, 0, data.length, compressedOut, 0);
        byte[] trimmedBuffer = Arrays.copyOf(compressedOut, compressedSize);
        return trimmedBuffer;
    }
}
