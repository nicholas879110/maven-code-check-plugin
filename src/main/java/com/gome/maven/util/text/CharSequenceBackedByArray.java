package com.gome.maven.util.text;

/**
 * A char sequence base on a char array, Gives access to that array. May be used for performance optimizations
 *
 * @author Maxim.Mossienko
 * @see com.gome.maven.util.text.CharArrayExternalizable
 * @see com.gome.maven.util.text.CharArrayUtil#getChars(CharSequence, char[], int)
 * @see com.gome.maven.util.text.CharArrayUtil#fromSequenceWithoutCopying(CharSequence)
 */
public interface CharSequenceBackedByArray extends CharSequence {
    char[] getChars();

    void getChars( char[] dst, int dstOffset);
}