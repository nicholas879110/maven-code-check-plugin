package com.gome.maven.openapi.util.text;

/**
 * @author zhangliewei
 * @date 2017/12/15 18:33
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class StringUtilRt {

    
    public static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

   
    
    public static CharSequence toUpperCase( CharSequence s) {
        StringBuilder answer = null;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char upcased = toUpperCase(c);
            if (answer == null && upcased != c) {
                answer = new StringBuilder(s.length());
                answer.append(s.subSequence(0, i));
            }

            if (answer != null) {
                answer.append(upcased);
            }
        }

        return answer == null ? s : answer;
    }

    
    public static char toUpperCase(char a) {
        if (a < 'a') {
            return a;
        }
        if (a <= 'z') {
            return (char)(a + ('A' - 'a'));
        }
        return Character.toUpperCase(a);
    }

    
    public static char toLowerCase(char a) {
        if (a < 'A' || a >= 'a' && a <= 'z') {
            return a;
        }

        if (a <= 'Z') {
            return (char)(a + ('a' - 'A'));
        }

        return Character.toLowerCase(a);
    }

    /**
     * Converts line separators to <code>"\n"</code>
     */
   
    
    public static String convertLineSeparators( String text) {
        return convertLineSeparators(text, false);
    }

   
    
    public static String convertLineSeparators( String text, boolean keepCarriageReturn) {
        return convertLineSeparators(text, "\n", null, keepCarriageReturn);
    }

   
    
    public static String convertLineSeparators( String text, String newSeparator) {
        return convertLineSeparators(text, newSeparator, null);
    }

   
    
    public static CharSequence convertLineSeparators( CharSequence text, String newSeparator) {
        return unifyLineSeparators(text, newSeparator, null, false);
    }

   
    public static String convertLineSeparators( String text, String newSeparator, int[] offsetsToKeep) {
        return convertLineSeparators(text, newSeparator, offsetsToKeep, false);
    }

   
    public static String convertLineSeparators( String text,
                                               String newSeparator,
                                               int[] offsetsToKeep,
                                               boolean keepCarriageReturn) {
        return unifyLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn).toString();
    }

   
    
    public static CharSequence unifyLineSeparators( CharSequence text) {
        return unifyLineSeparators(text, "\n", null, false);
    }

   
    public static CharSequence unifyLineSeparators( CharSequence text,
                                                   String newSeparator,
                                                   int[] offsetsToKeep,
                                                   boolean keepCarriageReturn) {
        StringBuilder buffer = null;
        int intactLength = 0;
        final boolean newSeparatorIsSlashN = "\n".equals(newSeparator);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                if (!newSeparatorIsSlashN) {
                    if (buffer == null) {
                        buffer = new StringBuilder(text.length());
                        buffer.append(text, 0, intactLength);
                    }
                    buffer.append(newSeparator);
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
                else if (buffer == null) {
                    intactLength++;
                }
                else {
                    buffer.append(c);
                }
            }
            else if (c == '\r') {
                boolean followedByLineFeed = i < text.length() - 1 && text.charAt(i + 1) == '\n';
                if (!followedByLineFeed && keepCarriageReturn) {
                    if (buffer == null) {
                        intactLength++;
                    }
                    else {
                        buffer.append(c);
                    }
                    continue;
                }
                if (buffer == null) {
                    buffer = new StringBuilder(text.length());
                    buffer.append(text, 0, intactLength);
                }
                buffer.append(newSeparator);
                if (followedByLineFeed) {
                    //noinspection AssignmentToForLoopParameter
                    i++;
                    shiftOffsets(offsetsToKeep, buffer.length(), 2, newSeparator.length());
                }
                else {
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
            }
            else {
                if (buffer == null) {
                    intactLength++;
                }
                else {
                    buffer.append(c);
                }
            }
        }
        return buffer == null ? text : buffer;
    }

    private static void shiftOffsets(int[] offsets, int changeOffset, int oldLength, int newLength) {
        if (offsets == null) return;
        int shift = newLength - oldLength;
        if (shift == 0) return;
        for (int i = 0; i < offsets.length; i++) {
            int offset = offsets[i];
            if (offset >= changeOffset + oldLength) {
                offsets[i] += shift;
            }
        }
    }

    
    public static int parseInt( String string, final int defaultValue) {
        if (string == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    
    public static double parseDouble(final String string, final double defaultValue) {
        try {
            return Double.parseDouble(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    
    public static boolean parseBoolean(final String string, final boolean defaultValue) {
        try {
            return Boolean.parseBoolean(string);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

   
    
    public static String getShortName( Class aClass) {
        return getShortName(aClass.getName());
    }

   
    
    public static String getShortName( String fqName) {
        return getShortName(fqName, '.');
    }

   
    
    public static String getShortName( String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    
    public static boolean endsWithChar( CharSequence s, char suffix) {
        return s != null && s.length() != 0 && s.charAt(s.length() - 1) == suffix;
    }

    
    public static boolean startsWithIgnoreCase( String str,  String prefix) {
        final int stringLength = str.length();
        final int prefixLength = prefix.length();
        return stringLength >= prefixLength && str.regionMatches(true, 0, prefix, 0, prefixLength);
    }

    
    public static boolean endsWithIgnoreCase( CharSequence text,  CharSequence suffix) {
        int l1 = text.length();
        int l2 = suffix.length();
        if (l1 < l2) return false;

        for (int i = l1 - 1; i >= l1 - l2; i--) {
            if (!charsEqualIgnoreCase(text.charAt(i), suffix.charAt(i + l2 - l1))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Allows to retrieve index of last occurrence of the given symbols at <code>[start; end)</code> sub-sequence of the given text.
     *
     * @param s     target text
     * @param c     target symbol which last occurrence we want to check
     * @param start start offset of the target text (inclusive)
     * @param end   end offset of the target text (exclusive)
     * @return index of the last occurrence of the given symbol at the target sub-sequence of the given text if any;
     * <code>-1</code> otherwise
     */
    
    public static int lastIndexOf( CharSequence s, char c, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (s.charAt(i) == c) return i;
        }
        return -1;
    }
}
