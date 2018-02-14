package com.gome.maven.util.text;

import com.gome.maven.openapi.util.text.StringUtil;
/**
 * @author zhangliewei
 * @date 2017/12/29 16:02
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class CharArrayCharSequence implements CharSequenceBackedByArray {
    protected final char[] myChars;
    protected final int myStart;
    protected final int myEnd;

    public CharArrayCharSequence( char... chars) {
        this(chars, 0, chars.length);
    }

    public CharArrayCharSequence( char[] chars, int start, int end) {
        if (start < 0 || end > chars.length || start > end) {
            throw new IndexOutOfBoundsException("chars.length:" + chars.length +
                    ", start:" + start +
                    ", end:" + end);
        }
        myChars = chars;
        myStart = start;
        myEnd = end;
    }

    @Override
    public final int length() {
        return myEnd - myStart;
    }

    @Override
    public final char charAt(int index) {
        return myChars[index + myStart];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return start == 0 && end == length() ? this : new CharArrayCharSequence(myChars, myStart + start, myStart + end);
    }

    @Override
    
    public String toString() {
        return new String(myChars, myStart, myEnd - myStart); //TODO StringFactory
    }

    @Override
    public char[] getChars() {
        if (myStart == 0 /*&& myEnd == myChars.length*/) return myChars;
        char[] chars = new char[length()];
        System.arraycopy(myChars, myStart, chars, 0, length());
        return chars;
    }

    @Override
    public int hashCode() {
        return StringUtil.stringHashCode(myChars, myStart, myEnd);
    }

    @Override
    public void getChars( char[] dst, int dstOffset) {
        System.arraycopy(myChars, myStart, dst, dstOffset, length());
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof CharSequence) {
            CharSequence anotherString = (CharSequence)anObject;
            int n = myEnd - myStart;
            if (n == anotherString.length()) {
                for (int i = 0; i < n; i++) {
                    if (myChars[myStart + i] != anotherString.charAt(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * See {@link java.io.Reader#read(char[], int, int)};
     */
    public int readCharsTo(int start, char[] cbuf, int off, int len) {
        final int readChars = Math.min(len, length() - start);
        if (readChars <= 0) return -1;

        System.arraycopy(myChars, myStart + start, cbuf, off, readChars);
        return readChars;
    }
}