//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

import java.nio.ByteOrder;

final class SnappyInternalUtils {
    private static final Memory memory;
    static final boolean HAS_UNSAFE;

    private SnappyInternalUtils() {
    }

    static boolean equals(byte[] left, int leftIndex, byte[] right, int rightIndex, int length) {
        checkPositionIndexes(leftIndex, leftIndex + length, left.length);
        checkPositionIndexes(rightIndex, rightIndex + length, right.length);

        for(int i = 0; i < length; ++i) {
            if (left[leftIndex + i] != right[rightIndex + i]) {
                return false;
            }
        }

        return true;
    }

    public static int lookupShort(short[] data, int index) {
        return memory.lookupShort(data, index);
    }

    public static int loadByte(byte[] data, int index) {
        return memory.loadByte(data, index);
    }

    static int loadInt(byte[] data, int index) {
        return memory.loadInt(data, index);
    }

    static void copyLong(byte[] src, int srcIndex, byte[] dest, int destIndex) {
        memory.copyLong(src, srcIndex, dest, destIndex);
    }

    static long loadLong(byte[] data, int index) {
        return memory.loadLong(data, index);
    }

    static void copyMemory(byte[] input, int inputIndex, byte[] output, int outputIndex, int length) {
        memory.copyMemory(input, inputIndex, output, outputIndex, length);
    }

    static <T> T checkNotNull(T reference, String errorMessageTemplate, Object... errorMessageArgs) {
        if (reference == null) {
            throw new NullPointerException(String.format(errorMessageTemplate, errorMessageArgs));
        } else {
            return reference;
        }
    }

    static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    static void checkPositionIndexes(int start, int end, int size) {
        if (start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
        }
    }

    static String badPositionIndexes(int start, int end, int size) {
        if (start >= 0 && start <= size) {
            return end >= 0 && end <= size ? String.format("end index (%s) must not be less than start index (%s)", end, start) : badPositionIndex(end, size, "end index");
        } else {
            return badPositionIndex(start, size, "start index");
        }
    }

    static String badPositionIndex(int index, int size, String desc) {
        if (index < 0) {
            return String.format("%s (%s) must not be negative", desc, index);
        } else if (size < 0) {
            throw new IllegalArgumentException("negative size: " + size);
        } else {
            return String.format("%s (%s) must not be greater than size (%s)", desc, index, size);
        }
    }

    static {
        Memory memoryInstance = null;
        Class slowMemoryClass;
        Memory slowMemory;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            try {
                slowMemoryClass = SnappyInternalUtils.class.getClassLoader().loadClass("org.iq80.snappy.UnsafeMemory").asSubclass(Memory.class);
                slowMemory = (Memory)slowMemoryClass.newInstance();
                if (slowMemory.loadInt(new byte[4], 0) == 0) {
                    memoryInstance = slowMemory;
                }
            } catch (Throwable var4) {
                ;
            }
        }

        if (memoryInstance == null) {
            try {
                slowMemoryClass = SnappyInternalUtils.class.getClassLoader().loadClass("org.iq80.snappy.SlowMemory").asSubclass(Memory.class);
                slowMemory = (Memory)slowMemoryClass.newInstance();
                if (slowMemory.loadInt(new byte[4], 0) != 0) {
                    throw new AssertionError("SlowMemory class is broken!");
                }

                memoryInstance = slowMemory;
            } catch (Throwable var3) {
                throw new AssertionError("Could not find SlowMemory class");
            }
        }

        memory = memoryInstance;
        HAS_UNSAFE = memory.fastAccessSupported();
    }
}
