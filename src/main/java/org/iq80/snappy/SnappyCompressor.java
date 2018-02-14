//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.nio.ByteOrder;
import java.util.Arrays;

final class SnappyCompressor {
    private static final boolean NATIVE_LITTLE_ENDIAN;
    private static final int BLOCK_LOG = 15;
    private static final int BLOCK_SIZE = 32768;
    private static final int INPUT_MARGIN_BYTES = 15;
    private static final int MAX_HASH_TABLE_BITS = 14;
    private static final int MAX_HASH_TABLE_SIZE = 16384;

    SnappyCompressor() {
    }

    public static int maxCompressedLength(int sourceLength) {
        return 32 + sourceLength + sourceLength / 6;
    }

    public static int compress(byte[] uncompressed, int uncompressedOffset, int uncompressedLength, byte[] compressed, int compressedOffset) {
        int compressedIndex = writeUncompressedLength(compressed, compressedOffset, uncompressedLength);
        int hashTableSize = getHashTableSize(uncompressedLength);
        BufferRecycler recycler = BufferRecycler.instance();
        short[] table = recycler.allocEncodingHash(hashTableSize);

        for(int read = 0; read < uncompressedLength; read += 32768) {
            Arrays.fill(table, (short)0);
            compressedIndex = compressFragment(uncompressed, uncompressedOffset + read, Math.min(uncompressedLength - read, 32768), compressed, compressedIndex, table);
        }

        recycler.releaseEncodingHash(table);
        return compressedIndex - compressedOffset;
    }

    private static int compressFragment(byte[] input, int inputOffset, int inputSize, byte[] output, int outputIndex, short[] table) {
        int ipIndex = inputOffset;

        assert inputSize <= 32768;

        int ipEndIndex = inputOffset + inputSize;
        int hashTableSize = getHashTableSize(inputSize);
        int shift = 32 - log2Floor(hashTableSize);

        assert (hashTableSize & hashTableSize - 1) == 0 : "table must be power of two";

        assert -1 >>> shift == hashTableSize - 1;

        int nextEmitIndex = inputOffset;
        if (inputSize >= 15) {
            for(int ipLimit = inputOffset + inputSize - 15; ipIndex <= ipLimit; nextEmitIndex = ipIndex) {
                assert nextEmitIndex <= ipIndex;

                int skip = 32;
                int[] candidateResult = findCandidate(input, ipIndex, ipLimit, inputOffset, shift, table, skip);
                ipIndex = candidateResult[0];
                int candidateIndex = candidateResult[1];
                 skip = candidateResult[2];
                if (ipIndex + bytesBetweenHashLookups(skip) > ipLimit) {
                    break;
                }

                assert nextEmitIndex + 16 <= ipEndIndex;

                outputIndex = emitLiteral(output, outputIndex, input, nextEmitIndex, ipIndex - nextEmitIndex, true);
                int[] indexes = emitCopies(input, inputOffset, inputSize, ipIndex, output, outputIndex, table, shift, candidateIndex);
                ipIndex = indexes[0];
                outputIndex = indexes[1];
            }
        }

        if (nextEmitIndex < ipEndIndex) {
            outputIndex = emitLiteral(output, outputIndex, input, nextEmitIndex, ipEndIndex - nextEmitIndex, false);
        }

        return outputIndex;
    }

    private static int[] findCandidate(byte[] input, int ipIndex, int ipLimit, int inputOffset, int shift, short[] table, int skip) {
        int candidateIndex = 0;
        ++ipIndex;

        while(ipIndex + bytesBetweenHashLookups(skip) <= ipLimit) {
            int currentInt = SnappyInternalUtils.loadInt(input, ipIndex);
            int hash = hashBytes(currentInt, shift);
            candidateIndex = inputOffset + table[hash];

            assert candidateIndex >= 0;

            assert candidateIndex < ipIndex;

            table[hash] = (short)(ipIndex - inputOffset);
            if (currentInt == SnappyInternalUtils.loadInt(input, candidateIndex)) {
                break;
            }

            ipIndex += bytesBetweenHashLookups(skip++);
        }

        return new int[]{ipIndex, candidateIndex, skip};
    }

    private static int bytesBetweenHashLookups(int skip) {
        return skip >>> 5;
    }

    private static int[] emitCopies(byte[] input, int inputOffset, int inputSize, int ipIndex, byte[] output, int outputIndex, short[] table, int shift, int candidateIndex) {
        int inputBytes;
        do {
            int matched = 4 + findMatchLength(input, candidateIndex + 4, input, ipIndex + 4, inputOffset + inputSize);
            int offset = ipIndex - candidateIndex;

            assert SnappyInternalUtils.equals(input, ipIndex, input, candidateIndex, matched);

            ipIndex += matched;
            outputIndex = emitCopy(output, outputIndex, offset, matched);
            if (ipIndex >= inputOffset + inputSize - 15) {
                return new int[]{ipIndex, outputIndex};
            }

            int prevInt;
            if (SnappyInternalUtils.HAS_UNSAFE) {
                long foo = SnappyInternalUtils.loadLong(input, ipIndex - 1);
                prevInt = (int)foo;
                inputBytes = (int)(foo >>> 8);
            } else {
                prevInt = SnappyInternalUtils.loadInt(input, ipIndex - 1);
                inputBytes = SnappyInternalUtils.loadInt(input, ipIndex);
            }

            int prevHash = hashBytes(prevInt, shift);
            table[prevHash] = (short)(ipIndex - inputOffset - 1);
            int curHash = hashBytes(inputBytes, shift);
            candidateIndex = inputOffset + table[curHash];
            table[curHash] = (short)(ipIndex - inputOffset);
        } while(inputBytes == SnappyInternalUtils.loadInt(input, candidateIndex));

        return new int[]{ipIndex, outputIndex};
    }

    private static int emitLiteral(byte[] output, int outputIndex, byte[] literal, int literalIndex, int length, boolean allowFastPath) {
        SnappyInternalUtils.checkPositionIndexes(literalIndex, literalIndex + length, literal.length);
        int n = length - 1;
        if (n < 60) {
            output[outputIndex++] = (byte)(0 | n << 2);
            if (allowFastPath && length <= 16) {
                SnappyInternalUtils.copyLong(literal, literalIndex, output, outputIndex);
                SnappyInternalUtils.copyLong(literal, literalIndex + 8, output, outputIndex + 8);
                outputIndex += length;
                return outputIndex;
            }
        } else if (n < 256) {
            output[outputIndex++] = -16;
            output[outputIndex++] = (byte)n;
        } else if (n < 65536) {
            output[outputIndex++] = -12;
            output[outputIndex++] = (byte)n;
            output[outputIndex++] = (byte)(n >>> 8);
        } else if (n < 16777216) {
            output[outputIndex++] = -8;
            output[outputIndex++] = (byte)n;
            output[outputIndex++] = (byte)(n >>> 8);
            output[outputIndex++] = (byte)(n >>> 16);
        } else {
            output[outputIndex++] = -4;
            output[outputIndex++] = (byte)n;
            output[outputIndex++] = (byte)(n >>> 8);
            output[outputIndex++] = (byte)(n >>> 16);
            output[outputIndex++] = (byte)(n >>> 24);
        }

        SnappyInternalUtils.checkPositionIndexes(literalIndex, literalIndex + length, literal.length);
        System.arraycopy(literal, literalIndex, output, outputIndex, length);
        outputIndex += length;
        return outputIndex;
    }

    private static int emitCopyLessThan64(byte[] output, int outputIndex, int offset, int length) {
        assert offset >= 0;

        assert length <= 64;

        assert length >= 4;

        assert offset < 65536;

        if (length < 12 && offset < 2048) {
            int lenMinus4 = length - 4;

            assert lenMinus4 < 8;

            output[outputIndex++] = (byte)(1 | lenMinus4 << 2 | offset >>> 8 << 5);
            output[outputIndex++] = (byte)offset;
        } else {
            output[outputIndex++] = (byte)(2 | length - 1 << 2);
            output[outputIndex++] = (byte)offset;
            output[outputIndex++] = (byte)(offset >>> 8);
        }

        return outputIndex;
    }

    private static int emitCopy(byte[] output, int outputIndex, int offset, int length) {
        while(length >= 68) {
            outputIndex = emitCopyLessThan64(output, outputIndex, offset, 64);
            length -= 64;
        }

        if (length > 64) {
            outputIndex = emitCopyLessThan64(output, outputIndex, offset, 60);
            length -= 60;
        }

        outputIndex = emitCopyLessThan64(output, outputIndex, offset, length);
        return outputIndex;
    }

    private static int findMatchLength(byte[] s1, int s1Index, byte[] s2, int s2Index, int s2Limit) {
        assert s2Limit >= s2Index;

        int matched;
        if (!SnappyInternalUtils.HAS_UNSAFE) {
            matched = s2Limit - s2Index;

            for(matched = 0; matched < matched; ++matched) {
                if (s1[s1Index + matched] != s2[s2Index + matched]) {
                    return matched;
                }
            }

            return matched;
        } else {
            for(matched = 0; s2Index + matched <= s2Limit - 4 && SnappyInternalUtils.loadInt(s2, s2Index + matched) == SnappyInternalUtils.loadInt(s1, s1Index + matched); matched += 4) {
                ;
            }

            if (NATIVE_LITTLE_ENDIAN && s2Index + matched <= s2Limit - 4) {
                matched = SnappyInternalUtils.loadInt(s2, s2Index + matched) ^ SnappyInternalUtils.loadInt(s1, s1Index + matched);
                int matchingBits = Integer.numberOfTrailingZeros(matched);
                matched += matchingBits >> 3;
            } else {
                while(s2Index + matched < s2Limit && s1[s1Index + matched] == s2[s2Index + matched]) {
                    ++matched;
                }
            }

            return matched;
        }
    }

    private static int getHashTableSize(int inputSize) {
        int hashTableSize;
        for(hashTableSize = 256; hashTableSize < 16384 && hashTableSize < inputSize; hashTableSize <<= 1) {
            ;
        }

        assert 0 == (hashTableSize & hashTableSize - 1) : "hash must be power of two";

        assert hashTableSize <= 16384 : "hash table too large";

        return hashTableSize;
    }

    private static int hashBytes(int bytes, int shift) {
        int kMul = 506832829;
        return bytes * kMul >>> shift;
    }

    private static int log2Floor(int n) {
        return n == 0 ? -1 : 31 ^ Integer.numberOfLeadingZeros(n);
    }

    private static int writeUncompressedLength(byte[] compressed, int compressedOffset, int uncompressedLength) {
        int highBitMask = 128;
        if (uncompressedLength < 128 && uncompressedLength >= 0) {
            compressed[compressedOffset++] = (byte)uncompressedLength;
        } else if (uncompressedLength < 16384 && uncompressedLength > 0) {
            compressed[compressedOffset++] = (byte)(uncompressedLength | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 7);
        } else if (uncompressedLength < 2097152 && uncompressedLength > 0) {
            compressed[compressedOffset++] = (byte)(uncompressedLength | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 7 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 14);
        } else if (uncompressedLength < 268435456 && uncompressedLength > 0) {
            compressed[compressedOffset++] = (byte)(uncompressedLength | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 7 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 14 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 21);
        } else {
            compressed[compressedOffset++] = (byte)(uncompressedLength | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 7 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 14 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 21 | highBitMask);
            compressed[compressedOffset++] = (byte)(uncompressedLength >>> 28);
        }

        return compressedOffset;
    }

    static {
        NATIVE_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    }
}
