//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.iq80.snappy;

interface Memory {
    boolean fastAccessSupported();

    int lookupShort(short[] var1, int var2);

    int loadByte(byte[] var1, int var2);

    int loadInt(byte[] var1, int var2);

    void copyLong(byte[] var1, int var2, byte[] var3, int var4);

    long loadLong(byte[] var1, int var2);

    void copyMemory(byte[] var1, int var2, byte[] var3, int var4, int var5);
}
