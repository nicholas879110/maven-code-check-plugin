/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.spellchecker.compress;


import java.util.Arrays;

public class UnitBitSet {
    public static final int MAX_CHARS_IN_WORD = 64;
    public static final int MAX_UNIT_VALUE = 255;

    final byte[] b;
    private final Alphabet alpha;

    public UnitBitSet( byte[] indices,  Alphabet alphabet) {
        b = indices;
        alpha = alphabet;
    }

    public int getUnitValue(int number) {
        final int r = b[number] & 0xFF;
        assert r >= 0 && r <= MAX_UNIT_VALUE : "invalid unit value";
        return r;
    }

    public void setUnitValue(int number, int value) {
        assert value >= 0 : "unit value is negative" + value;
        assert value <= MAX_UNIT_VALUE : "unit value is too big";
        b[number] = (byte)value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnitBitSet)) return false;
        return Arrays.equals(b, ((UnitBitSet)obj).b);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        for (byte aB : b) {
            s.append(Integer.toHexString((int)aB & 0xFF));
        }
        return s.toString();
    }

    
    public byte[] pack() {
        int meaningfulBits = 32 - Integer.numberOfLeadingZeros(alpha.getLastIndexUsed());
        assert meaningfulBits <= 8 && meaningfulBits >= 1 : meaningfulBits + ": "+alpha.getLastIndexUsed();
        byte[] result = new byte[(b.length * meaningfulBits + 7) / 8];

        int byteNumber = 0;
        int bitOffset = 0;

        for (byte index : b) {
            int bitsToChip = Math.min(8 - bitOffset, meaningfulBits);
            result[byteNumber] |= (index & ((1 << bitsToChip) - 1)) << bitOffset;

            int bitsLeft = meaningfulBits - bitsToChip;
            if (bitsLeft > 0) {
                byteNumber++;
                result[byteNumber] |= (index >> bitsToChip) & ((1 << bitsLeft) - 1);
                bitOffset = bitsLeft;
            }
            else {
                bitOffset += bitsToChip;
            }
        }
        return result;
    }

    
    public static String decode( byte[] packed,  Alphabet alphabet) {
        int meaningfulBits = 32 - Integer.numberOfLeadingZeros(alphabet.getLastIndexUsed());
        assert meaningfulBits <= 8;

        StringBuilder result = new StringBuilder(packed.length * 8 / meaningfulBits);

        int curByte = packed[0];
        int byteIndex = 0;
        int bitOffset = 0;

        while (byteIndex < packed.length) {
            int index = curByte & ((1 << meaningfulBits) - 1);
            char letter = alphabet.getLetter(index);
            if (letter == '\u0000') {
                break;
            }
            result.append(letter);

            curByte >>>= meaningfulBits;
            bitOffset += meaningfulBits;
            assert bitOffset <= 8;
            if (bitOffset + meaningfulBits > 8) {
                if (++byteIndex == packed.length) break;
                int leftOverBits = 8 - bitOffset;
                curByte = packed[byteIndex] << leftOverBits | (curByte & ((1 << leftOverBits) - 1));
                bitOffset = -leftOverBits;
            }
        }
        return result.toString();
    }

    public static int getFirstLetterIndex(byte firstPackedByte,  Alphabet alphabet) {
        int meaningfulBits = 32 - Integer.numberOfLeadingZeros(alphabet.getLastIndexUsed());
        assert meaningfulBits <= 8;

        int index = firstPackedByte & ((1 << meaningfulBits) - 1);
        return index;
    }
}
