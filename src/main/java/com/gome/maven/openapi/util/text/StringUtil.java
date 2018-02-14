/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.util.text;



import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.util.LineSeparator;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.text.*;

import java.beans.Introspector;
import java.io.IOException;
import java.util.*;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TeamCity inherits StringUtil: do not add private constructors!!!
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "MethodOverridesStaticMethodOfSuperclass"})
public class StringUtil extends StringUtilRt {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.text.StringUtil");

     private static final String VOWELS = "aeiouy";
     private static final Pattern EOL_SPLIT_KEEP_SEPARATORS = Pattern.compile("(?<=(\r\n|\n))|(?<=\r)(?=[^\n])");
     private static final Pattern EOL_SPLIT_PATTERN = Pattern.compile(" *(\r|\n|\r\n)+ *");
     private static final Pattern EOL_SPLIT_PATTERN_WITH_EMPTY = Pattern.compile(" *(\r|\n|\r\n) *");
     private static final Pattern EOL_SPLIT_DONT_TRIM_PATTERN = Pattern.compile("(\r|\n|\r\n)+");

    public static final NotNullFunction<String, String> QUOTER = new NotNullFunction<String, String>() {
        @Override
        
        public String fun(String s) {
            return "\"" + s + "\"";
        }
    };

    public static final NotNullFunction<String, String> SINGLE_QUOTER = new NotNullFunction<String, String>() {
        @Override
        
        public String fun(String s) {
            return "'" + s + "'";
        }
    };

    
    
    public static List<String> getWordsInStringLongestFirst( String find) {
        List<String> words = getWordsIn(find);
        // hope long words are rare
        Collections.sort(words, new Comparator<String>() {
            @Override
            public int compare( final String o1,  final String o2) {
                return o2.length() - o1.length();
            }
        });
        return words;
    }

    
    
    public static String escapePattern( final String text) {
        return replace(replace(text, "'", "''"), "{", "'{'");
    }

    
    
    public static <T> Function<T, String> createToStringFunction(Class<T> cls) {
        return new Function<T, String>() {
            @Override
            public String fun( T o) {
                return o.toString();
            }
        };
    }

    
    public static Function<String, String> TRIMMER = new Function<String, String>() {
        
        @Override
        public String fun( String s) {
            return trim(s);
        }
    };

    
    
    public static String replace(  String text,   String oldS,   String newS) {
        return replace(text, oldS, newS, false);
    }

    
    
    public static String replaceIgnoreCase(  String text,   String oldS,   String newS) {
        return replace(text, oldS, newS, true);
    }

    public static void replaceChar( char[] buffer, char oldChar, char newChar, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = buffer[i];
            if (c == oldChar) {
                buffer[i] = newChar;
            }
        }
    }

    
    
    public static String replaceChar( String buffer, char oldChar, char newChar) {
        StringBuilder newBuffer = null;
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (c == oldChar) {
                if (newBuffer == null) {
                    newBuffer = new StringBuilder(buffer.length());
                    newBuffer.append(buffer, 0, i);
                }

                newBuffer.append(newChar);
            }
            else if (newBuffer != null) {
                newBuffer.append(c);
            }
        }
        return newBuffer == null ? buffer : newBuffer.toString();
    }

    
    public static String replace(  final String text,   final String oldS,   final String newS, final boolean ignoreCase) {
        if (text.length() < oldS.length()) return text;

        StringBuilder newText = null;
        int i = 0;

        while (i < text.length()) {
            final int index = ignoreCase? indexOfIgnoreCase(text, oldS, i) : text.indexOf(oldS, i);
            if (index < 0) {
                if (i == 0) {
                    return text;
                }

                newText.append(text, i, text.length());
                break;
            }
            else {
                if (newText == null) {
                    if (text.length() == oldS.length()) {
                        return newS;
                    }
                    newText = new StringBuilder(text.length() - i);
                }

                newText.append(text, i, index);
                newText.append(newS);
                i = index + oldS.length();
            }
        }
        return newText != null ? newText.toString() : "";
    }

    /**
     * Implementation copied from {@link String#indexOf(String, int)} except character comparisons made case insensitive
     */
    
    public static int indexOfIgnoreCase( String where,  String what, int fromIndex) {
        int targetCount = what.length();
        int sourceCount = where.length();

        if (fromIndex >= sourceCount) {
            return targetCount == 0 ? sourceCount : -1;
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        if (targetCount == 0) {
            return fromIndex;
        }

        char first = what.charAt(0);
        int max = sourceCount - targetCount;

        for (int i = fromIndex; i <= max; i++) {
      /* Look for first character. */
            if (!charsEqualIgnoreCase(where.charAt(i), first)) {
                while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) ;
            }

      /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = 1; j < end && charsEqualIgnoreCase(where.charAt(j), what.charAt(k)); j++, k++) ;

                if (j == end) {
          /* Found whole string. */
                    return i;
                }
            }
        }

        return -1;
    }

    
    public static int indexOfIgnoreCase( String where, char what, int fromIndex) {
        int sourceCount = where.length();

        if (fromIndex >= sourceCount) {
            return -1;
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        for (int i = fromIndex; i < sourceCount; i++) {
            if (charsEqualIgnoreCase(where.charAt(i), what)) {
                return i;
            }
        }

        return -1;
    }

    
    public static boolean containsIgnoreCase( String where,  String what) {
        return indexOfIgnoreCase(where, what, 0) >= 0;
    }

    
    public static boolean endsWithIgnoreCase(  String str,   String suffix) {
        return StringUtilRt.endsWithIgnoreCase(str, suffix);
    }

    
    public static boolean startsWithIgnoreCase(  String str,   String prefix) {
        return StringUtilRt.startsWithIgnoreCase(str, prefix);
    }

    
    
    public static String stripHtml( String html, boolean convertBreaks) {
        if (convertBreaks) {
            html = html.replaceAll("<br/?>", "\n\n");
        }

        return html.replaceAll("<(.|\n)*?>", "");
    }

    public static String toLowerCase( final String str) {
        //noinspection ConstantConditions
        return str == null ? null : str.toLowerCase();
    }

    
    
    public static String getPackageName( String fqName) {
        return getPackageName(fqName, '.');
    }

    
    
    public static String getPackageName( String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    
    public static int getLineBreakCount( CharSequence text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                count++;
            }
            else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    //noinspection AssignmentToForLoopParameter
                    i++;
                    count++;
                }
                else {
                    count++;
                }
            }
        }
        return count;
    }

    
    public static boolean containsLineBreak( CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isLineBreak(c)) return true;
        }
        return false;
    }

    
    public static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    
    
    public static String escapeLineBreak( String text) {
        StringBuilder buffer = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }

    
    public static boolean endsWithLineBreak( CharSequence text) {
        int len = text.length();
        return len > 0 && isLineBreak(text.charAt(len - 1));
    }

    
    public static int lineColToOffset( CharSequence text, int line, int col) {
        int curLine = 0;
        int offset = 0;
        while (line != curLine) {
            if (offset == text.length()) return -1;
            char c = text.charAt(offset);
            if (c == '\n') {
                curLine++;
            }
            else if (c == '\r') {
                curLine++;
                if (offset < text.length() - 1 && text.charAt(offset + 1) == '\n') {
                    offset++;
                }
            }
            offset++;
        }
        return offset + col;
    }

    
    public static int offsetToLineNumber( CharSequence text, int offset) {
        int curLine = 0;
        int curOffset = 0;
        while (curOffset < offset) {
            if (curOffset == text.length()) return -1;
            char c = text.charAt(curOffset);
            if (c == '\n') {
                curLine++;
            }
            else if (c == '\r') {
                curLine++;
                if (curOffset < text.length() - 1 && text.charAt(curOffset + 1) == '\n') {
                    curOffset++;
                }
            }
            curOffset++;
        }
        return curLine;
    }

    /**
     * Classic dynamic programming algorithm for string differences.
     */
    
    public static int difference( String s1,  String s2) {
        int[][] a = new int[s1.length()][s2.length()];

        for (int i = 0; i < s1.length(); i++) {
            a[i][0] = i;
        }

        for (int j = 0; j < s2.length(); j++) {
            a[0][j] = j;
        }

        for (int i = 1; i < s1.length(); i++) {
            for (int j = 1; j < s2.length(); j++) {

                a[i][j] = Math.min(Math.min(a[i - 1][j - 1] + (s1.charAt(i) == s2.charAt(j) ? 0 : 1), a[i - 1][j] + 1), a[i][j - 1] + 1);
            }
        }

        return a[s1.length() - 1][s2.length() - 1];
    }

    
    
    public static String wordsToBeginFromUpperCase( String s) {
        return fixCapitalization(s, ourPrepositions, true);
    }

    
    
    public static String wordsToBeginFromLowerCase( String s) {
        return fixCapitalization(s, ourPrepositions, false);
    }

    
    
    public static String toTitleCase( String s) {
        return fixCapitalization(s, ArrayUtil.EMPTY_STRING_ARRAY, true);
    }

    
    private static String fixCapitalization( String s,  String[] prepositions, boolean title) {
        StringBuilder buffer = null;
        for (int i = 0; i < s.length(); i++) {
            char prevChar = i == 0 ? ' ' : s.charAt(i - 1);
            char currChar = s.charAt(i);
            if (!Character.isLetterOrDigit(prevChar) && prevChar != '\'') {
                if (Character.isLetterOrDigit(currChar)) {
                    if (title || Character.isUpperCase(currChar)) {
                        int j = i;
                        for (; j < s.length(); j++) {
                            if (!Character.isLetterOrDigit(s.charAt(j))) {
                                break;
                            }
                        }
                        if (!isPreposition(s, i, j - 1, prepositions)) {
                            if (buffer == null) {
                                buffer = new StringBuilder(s);
                            }
                            buffer.setCharAt(i, title ? toUpperCase(currChar) : toLowerCase(currChar));
                        }
                    }
                }
            }
        }
        if (buffer == null) {
            return s;
        }
        else {
            return buffer.toString();
        }
    }

     private static final String[] ourPrepositions = {
            "a", "an", "and", "as", "at", "but", "by", "down", "for", "from", "if", "in", "into", "not", "of", "on", "onto", "or", "out", "over",
            "per", "nor", "the", "to", "up", "upon", "via", "with"
    };

    
    public static boolean isPreposition( String s, int firstChar, int lastChar) {
        return isPreposition(s, firstChar, lastChar, ourPrepositions);
    }

    
    public static boolean isPreposition( String s, int firstChar, int lastChar,  String[] prepositions) {
        for (String preposition : prepositions) {
            boolean found = false;
            if (lastChar - firstChar + 1 == preposition.length()) {
                found = true;
                for (int j = 0; j < preposition.length(); j++) {
                    if (!(toLowerCase(s.charAt(firstChar + j)) == preposition.charAt(j))) {
                        found = false;
                    }
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    
    
    public static NotNullFunction<String, String> escaper(final boolean escapeSlash,  final String additionalChars) {
        return new NotNullFunction<String, String>() {
            
            @Override
            public String fun( String dom) {
                final StringBuilder builder = new StringBuilder(dom.length());
                escapeStringCharacters(dom.length(), dom, additionalChars, escapeSlash, builder);
                return builder.toString();
            }
        };
    }


    public static void escapeStringCharacters(int length,  String str,   StringBuilder buffer) {
        escapeStringCharacters(length, str, "\"", buffer);
    }

    
    public static StringBuilder escapeStringCharacters(int length,
                                                        String str,
                                                        String additionalChars,
                                                         StringBuilder buffer) {
        return escapeStringCharacters(length, str, additionalChars, true, buffer);
    }

    
    public static StringBuilder escapeStringCharacters(int length,
                                                        String str,
                                                        String additionalChars,
                                                       boolean escapeSlash,
                                                         StringBuilder buffer) {
        char prev = 0;
        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            switch (ch) {
                case '\b':
                    buffer.append("\\b");
                    break;

                case '\t':
                    buffer.append("\\t");
                    break;

                case '\n':
                    buffer.append("\\n");
                    break;

                case '\f':
                    buffer.append("\\f");
                    break;

                case '\r':
                    buffer.append("\\r");
                    break;

                default:
                    if (escapeSlash && ch == '\\') {
                        buffer.append("\\\\");
                    }
                    else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
                        buffer.append("\\").append(ch);
                    }
                    else if (!isPrintableUnicode(ch)) {
                        CharSequence hexCode = StringUtilRt.toUpperCase(Integer.toHexString(ch));
                        buffer.append("\\u");
                        int paddingCount = 4 - hexCode.length();
                        while (paddingCount-- > 0) {
                            buffer.append(0);
                        }
                        buffer.append(hexCode);
                    }
                    else {
                        buffer.append(ch);
                    }
            }
            prev = ch;
        }
        return buffer;
    }

    
    private static boolean isPrintableUnicode(char c) {
        int t = Character.getType(c);
        return t != Character.UNASSIGNED && t != Character.LINE_SEPARATOR && t != Character.PARAGRAPH_SEPARATOR &&
                t != Character.CONTROL && t != Character.FORMAT && t != Character.PRIVATE_USE && t != Character.SURROGATE;
    }

    
    
    public static String escapeStringCharacters( String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        escapeStringCharacters(s.length(), s, "\"", buffer);
        return buffer.toString();
    }

    
    
    public static String escapeCharCharacters( String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        escapeStringCharacters(s.length(), s, "\'", buffer);
        return buffer.toString();
    }

    
    
    public static String unescapeStringCharacters( String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        unescapeStringCharacters(s.length(), s, buffer);
        return buffer.toString();
    }

    private static boolean isQuoteAt( String s, int ind) {
        char ch = s.charAt(ind);
        return ch == '\'' || ch == '\"';
    }

    
    public static boolean isQuotedString( String s) {
        return s.length() > 1 && isQuoteAt(s, 0) && s.charAt(0) == s.charAt(s.length() - 1);
    }

    
    
    public static String unquoteString( String s) {
        if (isQuotedString(s)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    
    
    public static String unquoteString( String s, char quotationChar) {
        if (s.length() > 1 && quotationChar == s.charAt(0) && quotationChar == s.charAt(s.length() - 1)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * This is just an optimized version of Matcher.quoteReplacement
     */
    
    
    public static String quoteReplacement( String s) {
        boolean needReplacements = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                needReplacements = true;
                break;
            }
        }

        if (!needReplacements) return s;

        StringBuilder sb = new StringBuilder(s.length() * 6 / 5);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append('\\');
                sb.append('\\');
            }
            else if (c == '$') {
                sb.append('\\');
                sb.append('$');
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void unescapeStringCharacters(int length,  String s,  StringBuilder buffer) {
        boolean escaped = false;
        for (int idx = 0; idx < length; idx++) {
            char ch = s.charAt(idx);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                }
                else {
                    buffer.append(ch);
                }
            }
            else {
                switch (ch) {
                    case 'n':
                        buffer.append('\n');
                        break;

                    case 'r':
                        buffer.append('\r');
                        break;

                    case 'b':
                        buffer.append('\b');
                        break;

                    case 't':
                        buffer.append('\t');
                        break;

                    case 'f':
                        buffer.append('\f');
                        break;

                    case '\'':
                        buffer.append('\'');
                        break;

                    case '\"':
                        buffer.append('\"');
                        break;

                    case '\\':
                        buffer.append('\\');
                        break;

                    case 'u':
                        if (idx + 4 < length) {
                            try {
                                int code = Integer.parseInt(s.substring(idx + 1, idx + 5), 16);
                                idx += 4;
                                buffer.append((char)code);
                            }
                            catch (NumberFormatException e) {
                                buffer.append("\\u");
                            }
                        }
                        else {
                            buffer.append("\\u");
                        }
                        break;

                    default:
                        buffer.append(ch);
                        break;
                }
                escaped = false;
            }
        }

        if (escaped) buffer.append('\\');
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    
    
    public static String pluralize( String suggestion) {
        if (suggestion.endsWith("Child") || suggestion.endsWith("child")) {
            return suggestion + "ren";
        }

        if (suggestion.equals("this")) {
            return "these";
        }
        if (suggestion.equals("This")) {
            return "These";
        }

        if (endsWithIgnoreCase(suggestion, "es")) {
            return suggestion;
        }

        if (endsWithIgnoreCase(suggestion, "s") || endsWithIgnoreCase(suggestion, "x") || endsWithIgnoreCase(suggestion, "ch")) {
            return suggestion + "es";
        }

        int len = suggestion.length();
        if (endsWithIgnoreCase(suggestion, "y") && len > 1 && !isVowel(toLowerCase(suggestion.charAt(len - 2)))) {
            return suggestion.substring(0, len - 1) + "ies";
        }

        return suggestion + "s";
    }

    
    
    public static String capitalizeWords( String text,
                                         boolean allWords) {
        return capitalizeWords(text, " \t\n\r\f", allWords, false);
    }

    
    
    public static String capitalizeWords( String text,
                                          String tokenizerDelim,
                                         boolean allWords,
                                         boolean leaveOriginalDelims) {
        final StringTokenizer tokenizer = new StringTokenizer(text, tokenizerDelim, leaveOriginalDelims);
        final StringBuilder out = new StringBuilder(text.length());
        boolean toCapitalize = true;
        while (tokenizer.hasMoreTokens()) {
            final String word = tokenizer.nextToken();
            if (!leaveOriginalDelims && out.length() > 0) {
                out.append(' ');
            }
            out.append(toCapitalize ? capitalize(word) : word);
            if (!allWords) {
                toCapitalize = false;
            }
        }
        return out.toString();
    }

    
    public static String decapitalize(String s) {
        return Introspector.decapitalize(s);
    }

    
    public static boolean isVowel(char c) {
        return VOWELS.indexOf(c) >= 0;
    }

    
    
    public static String capitalize( String s) {
        if (s.isEmpty()) return s;
        if (s.length() == 1) return StringUtilRt.toUpperCase(s).toString();

        // Optimization
        if (Character.isUpperCase(s.charAt(0))) return s;
        return toUpperCase(s.charAt(0)) + s.substring(1);
    }


    public static boolean isCapitalized( String s) {
        return s != null && !s.isEmpty() && Character.isUpperCase(s.charAt(0));
    }

    
    
    public static String capitalizeWithJavaBeanConvention( String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return capitalize(s);
    }

    
    public static int stringHashCode( CharSequence chars) {
        if (chars instanceof String) return chars.hashCode();
        if (chars instanceof CharSequenceWithStringHash) return chars.hashCode();
        if (chars instanceof CharArrayCharSequence) return chars.hashCode();

        return stringHashCode(chars, 0, chars.length());
    }

    
    public static int stringHashCode( CharSequence chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars.charAt(off);
        }
        return h;
    }

    
    public static int stringHashCode(char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars[off];
        }
        return h;
    }

    
    public static int stringHashCodeInsensitive( char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + toLowerCase(chars[off]);
        }
        return h;
    }

    
    public static int stringHashCodeInsensitive( CharSequence chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + toLowerCase(chars.charAt(off));
        }
        return h;
    }

    
    public static int stringHashCodeInsensitive( CharSequence chars) {
        return stringHashCodeInsensitive(chars, 0, chars.length());
    }

    
    public static int stringHashCodeIgnoreWhitespaces(char[] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            char c = chars[off];
            if (!isWhiteSpace(c)) {
                h = 31 * h + c;
            }
        }
        return h;
    }

    
    public static int stringHashCodeIgnoreWhitespaces( CharSequence chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            char c = chars.charAt(off);
            if (!isWhiteSpace(c)) {
                h = 31 * h + c;
            }
        }
        return h;
    }

    
    public static int stringHashCodeIgnoreWhitespaces( CharSequence chars) {
        return stringHashCodeIgnoreWhitespaces(chars, 0, chars.length());
    }

    /**
     * Equivalent to string.startsWith(prefixes[0] + prefixes[1] + ...) but avoids creating an object for concatenation.
     */
    
    public static boolean startsWithConcatenation( String string,  String... prefixes) {
        int offset = 0;
        for (String prefix : prefixes) {
            int prefixLen = prefix.length();
            if (!string.regionMatches(offset, prefix, 0, prefixLen)) {
                return false;
            }
            offset += prefixLen;
        }
        return true;
    }

    /**
     * @deprecated use {@link #startsWithConcatenation(String, String...)} (to remove in IDEA 14).
     */
    @SuppressWarnings("UnusedDeclaration")
    
    public static boolean startsWithConcatenationOf( String string,  String firstPrefix,  String secondPrefix) {
        return startsWithConcatenation(string, firstPrefix, secondPrefix);
    }

    /**
     * @deprecated use {@link #startsWithConcatenation(String, String...)} (to remove in IDEA 14).
     */
    @SuppressWarnings("UnusedDeclaration")
    
    public static boolean startsWithConcatenationOf( String string,
                                                     String firstPrefix,
                                                     String secondPrefix,
                                                     String thirdPrefix) {
        return startsWithConcatenation(string, firstPrefix, secondPrefix, thirdPrefix);
    }

    public static String trim( String s) {
        return s == null ? null : s.trim();
    }

    
    
    public static String trimEnd( String s,   String suffix) {
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    
    
    public static String trimLog( final String text, final int limit) {
        if (limit > 5 && text.length() > limit) {
            return text.substring(0, limit - 5) + " ...\n";
        }
        return text;
    }

    
    
    public static String trimLeading( String string) {
        return trimLeading((CharSequence)string).toString();
    }
    
    
    public static CharSequence trimLeading( CharSequence string) {
        int index = 0;
        while (index < string.length() && Character.isWhitespace(string.charAt(index))) index++;
        return string.subSequence(index, string.length());
    }

    
    
    public static String trimLeading( String string, char symbol) {
        int index = 0;
        while (index < string.length() && string.charAt(index) == symbol) index++;
        return string.substring(index);
    }

    
    
    public static String trimTrailing( String string) {
        return trimTrailing((CharSequence)string).toString();
    }

    
    
    public static CharSequence trimTrailing( CharSequence string) {
        int index = string.length() - 1;
        while (index >= 0 && Character.isWhitespace(string.charAt(index))) index--;
        return string.subSequence(0, index + 1);
    }

    
    public static boolean startsWithChar( CharSequence s, char prefix) {
        return s != null && s.length() != 0 && s.charAt(0) == prefix;
    }

    
    public static boolean endsWithChar( CharSequence s, char suffix) {
        return StringUtilRt.endsWithChar(s, suffix);
    }

    
    
    public static String trimStart( String s,   String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    
    
    public static String pluralize( String base, int n) {
        if (n == 1) return base;
        return pluralize(base);
    }

    public static void repeatSymbol( Appendable buffer, char symbol, int times) {
        assert times >= 0 : times;
        try {
            for (int i = 0; i < times; i++) {
                buffer.append(symbol);
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    
    public static String defaultIfEmpty( String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    
    public static boolean isNotEmpty( String s) {
        return s != null && !s.isEmpty();
    }


    public static boolean isEmpty( String s) {
        return s == null || s.isEmpty();
    }


    public static boolean isEmpty( CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    
    public static int length( CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    
    
    public static String notNullize( final String s) {
        return notNullize(s, "");
    }

    
    
    public static String notNullize( final String s,  String defaultValue) {
        return s == null ? defaultValue : s;
    }

    
    
    public static String nullize( final String s) {
        return nullize(s, false);
    }

    
    
    public static String nullize( final String s, boolean nullizeSpaces) {
        if (nullizeSpaces) {
            if (isEmptyOrSpaces(s)) return null;
        }
        else {
            if (isEmpty(s)) return null;
        }
        return s;
    }


    // we need to keep this method to preserve backward compatibility
    public static boolean isEmptyOrSpaces( String s) {
        return isEmptyOrSpaces(((CharSequence)s));
    }


    public static boolean isEmptyOrSpaces( CharSequence s) {
        if (isEmpty(s)) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Allows to answer if given symbol is white space, tabulation or line feed.
     *
     * @param c symbol to check
     * @return <code>true</code> if given symbol is white space, tabulation or line feed; <code>false</code> otherwise
     */
    
    public static boolean isWhiteSpace(char c) {
        return c == '\n' || c == '\t' || c == ' ';
    }

    
    
    public static String getThrowableText( Throwable aThrowable) {
        return ExceptionUtil.getThrowableText(aThrowable);
    }

    
    
    public static String getThrowableText( Throwable aThrowable,   final String stackFrameSkipPattern) {
        return ExceptionUtil.getThrowableText(aThrowable, stackFrameSkipPattern);
    }

    
    
    public static String getMessage( Throwable e) {
        return ExceptionUtil.getMessage(e);
    }

    
    
    public static String repeatSymbol(final char aChar, final int count) {
        char[] buffer = new char[count];
        Arrays.fill(buffer, aChar);
        return StringFactory.createShared(buffer);
    }

    
    
    public static String repeat( String s, int count) {
        assert count >= 0 : count;
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    
    
    public static List<String> splitHonorQuotes( String s, char separator) {
        final List<String> result = new ArrayList<String>();
        final StringBuilder builder = new StringBuilder(s.length());
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == separator && !inQuotes) {
                if (builder.length() > 0) {
                    result.add(builder.toString());
                    builder.setLength(0);
                }
                continue;
            }

            if ((c == '"' || c == '\'') && !(i > 0 && s.charAt(i - 1) == '\\')) {
                inQuotes = !inQuotes;
            }
            builder.append(c);
        }

        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }


    
    
    public static List<String> split( String s,  String separator) {
        return split(s, separator, true);
    }
    
    
    public static List<CharSequence> split( CharSequence s,  CharSequence separator) {
        return split(s, separator, true, true);
    }

    
    
    public static List<String> split( String s,  String separator,
                                     boolean excludeSeparator) {
        return split(s, separator, excludeSeparator, true);
    }

    
    
    public static List<String> split( String s,  String separator,
                                     boolean excludeSeparator, boolean excludeEmptyStrings) {
        return (List)split((CharSequence)s,separator,excludeSeparator,excludeEmptyStrings);
    }
    
    
    public static List<CharSequence> split( CharSequence s,  CharSequence separator,
                                           boolean excludeSeparator, boolean excludeEmptyStrings) {
        if (separator.length() == 0) {
            return Collections.singletonList(s);
        }
        List<CharSequence> result = new ArrayList<CharSequence>();
        int pos = 0;
        while (true) {
            int index = indexOf(s,separator, pos);
            if (index == -1) break;
            final int nextPos = index + separator.length();
            CharSequence token = s.subSequence(pos, excludeSeparator ? index : nextPos);
            if (token.length() != 0 || !excludeEmptyStrings) {
                result.add(token);
            }
            pos = nextPos;
        }
        if (pos < s.length() || !excludeEmptyStrings && pos == s.length()) {
            result.add(s.subSequence(pos, s.length()));
        }
        return result;
    }

    
    
    public static Iterable<String> tokenize( String s,  String separators) {
        final StringTokenizer tokenizer = new StringTokenizer(s, separators);
        return new Iterable<String>() {
            
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return tokenizer.hasMoreTokens();
                    }

                    @Override
                    public String next() {
                        return tokenizer.nextToken();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    
    
    public static Iterable<String> tokenize( final StringTokenizer tokenizer) {
        return new Iterable<String>() {
            
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return tokenizer.hasMoreTokens();
                    }

                    @Override
                    public String next() {
                        return tokenizer.nextToken();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * @return list containing all words in {@code text}, or {@link ContainerUtil#emptyList()} if there are none.
     * The <b>word</b> here means the maximum sub-string consisting entirely of characters which are <code>Character.isJavaIdentifierPart(c)</code>.
     */
    
    
    public static List<String> getWordsIn( String text) {
        List<String> result = null;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isIdentifierPart = Character.isJavaIdentifierPart(c);
            if (isIdentifierPart && start == -1) {
                start = i;
            }
            if (isIdentifierPart && i == text.length() - 1 && start != -1) {
                if (result == null) {
                    result = new SmartList<String>();
                }
                result.add(text.substring(start, i + 1));
            }
            else if (!isIdentifierPart && start != -1) {
                if (result == null) {
                    result = new SmartList<String>();
                }
                result.add(text.substring(start, i));
                start = -1;
            }
        }
        if (result == null) {
            return ContainerUtil.emptyList();
        }
        return result;
    }

    
    
    public static List<TextRange> getWordIndicesIn(String text) {
        List<TextRange> result = new SmartList<TextRange>();
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isIdentifierPart = Character.isJavaIdentifierPart(c);
            if (isIdentifierPart && start == -1) {
                start = i;
            }
            if (isIdentifierPart && i == text.length() - 1 && start != -1) {
                result.add(new TextRange(start, i + 1));
            }
            else if (!isIdentifierPart && start != -1) {
                result.add(new TextRange(start, i));
                start = -1;
            }
        }
        return result;
    }

    
    
    public static String join( final String[] strings,  final String separator) {
        return join(strings, 0, strings.length, separator);
    }

    
    
    public static String join( final String[] strings, int startIndex, int endIndex,  final String separator) {
        final StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) result.append(separator);
            result.append(strings[i]);
        }
        return result.toString();
    }

    
    
    public static String[] zip( String[] strings1,  String[] strings2, String separator) {
        if (strings1.length != strings2.length) throw new IllegalArgumentException();

        String[] result = ArrayUtil.newStringArray(strings1.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = strings1[i] + separator + strings2[i];
        }

        return result;
    }

    
    
    public static String[] surround( String[] strings1, String prefix, String suffix) {
        String[] result = ArrayUtil.newStringArray(strings1.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = prefix + strings1[i] + suffix;
        }

        return result;
    }

    
    
    public static <T> String join( T[] items,  Function<T, String> f,   String separator) {
        return join(Arrays.asList(items), f, separator);
    }

    
    
    public static <T> String join( Collection<? extends T> items,
                                   Function<? super T, String> f,
                                    String separator) {
        if (items.isEmpty()) return "";
        return join((Iterable<? extends T>)items, f, separator);
    }

    
    public static String join( Iterable<?> items,   String separator) {
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            result.append(item).append(separator);
        }
        if (result.length() > 0) {
            result.setLength(result.length() - separator.length());
        }
        return result.toString();
    }

    
    
    public static <T> String join( Iterable<? extends T> items,
                                   Function<? super T, String> f,
                                    String separator) {
        final StringBuilder result = new StringBuilder();
        for (T item : items) {
            String string = f.fun(item);
            if (string != null && !string.isEmpty()) {
                if (result.length() != 0) result.append(separator);
                result.append(string);
            }
        }
        return result.toString();
    }

    
    
    public static String join( Collection<String> strings,  String separator) {
        if (strings.size() <= 1) {
            return notNullize(ContainerUtil.getFirstItem(strings));
        }
        StringBuilder result = new StringBuilder();
        join(strings, separator, result);
        return result.toString();
    }

    public static void join( Collection<String> strings,  String separator,  StringBuilder result) {
        boolean isFirst = true;
        for (String string : strings) {
            if (string != null) {
                if (isFirst) {
                    isFirst = false;
                }
                else {
                    result.append(separator);
                }
                result.append(string);
            }
        }
    }

    
    
    public static String join( final int[] strings,  final String separator) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) result.append(separator);
            result.append(strings[i]);
        }
        return result.toString();
    }

    
    
    public static String join( final String... strings) {
        if (strings == null || strings.length == 0) return "";

        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
            builder.append(string);
        }
        return builder.toString();
    }

    /**
     * Consider using {@link StringUtil#unquoteString(String)} instead.
     * Note: this method has an odd behavior:
     *   Quotes are removed even if leading and trailing quotes are different or
     *                           if there is only one quote (leading or trailing).
     */
    
    
    public static String stripQuotesAroundValue( String text) {
        final int len = text.length();
        if (len > 0) {
            final int from = isQuoteAt(text, 0) ? 1 : 0;
            final int to = len > 1 && isQuoteAt(text, len - 1) ? len - 1 : len;
            if (from > 0 || to < len) {
                return text.substring(from, to);
            }
        }
        return text;
    }

    /**
     * Formats the specified file size as a string.
     *
     * @param fileSize the size to format.
     * @return the size formatted as a string.
     * @since 5.0.1
     */
    
    
    public static String formatFileSize(long fileSize) {
        return formatValue(fileSize, null,
                new String[]{"B", "K", "M", "G", "T", "P", "E"},
                new long[]{1000, 1000, 1000, 1000, 1000, 1000});
    }

    
    
    public static String formatDuration(long duration) {
        return formatValue(duration, " ",
                new String[]{"ms", "s", "m", "h", "d", "w", "mo", "yr", "c", "ml", "ep"},
                new long[]{1000, 60, 60, 24, 7, 4, 12, 100, 10, 10000});
    }

    
    private static String formatValue(long value, String partSeparator, String[] units, long[] multipliers) {
        StringBuilder sb = new StringBuilder();
        long count = value;
        long remainder = 0;
        int i = 0;
        for (; i < units.length; i++) {
            long multiplier = i < multipliers.length ? multipliers[i] : -1;
            if (multiplier == -1 || count < multiplier) break;
            remainder = count % multiplier;
            count /= multiplier;
            if (partSeparator != null && (remainder != 0 || sb.length() > 0)) {
                sb.insert(0, units[i]).insert(0, remainder).insert(0, partSeparator);
            }
        }
        if (partSeparator != null || remainder == 0) {
            sb.insert(0, units[i]).insert(0, count);
        }
        else if (remainder > 0) {
            sb.append(String.format(Locale.US, "%.2f", count + (double)remainder / multipliers[i - 1])).append(units[i]);
        }
        return sb.toString();
    }

    /**
     * Returns unpluralized variant using English based heuristics like properties -> property, names -> name, children -> child.
     * Returns <code>null</code> if failed to match appropriate heuristic.
     *
     * @param name english word in plural form
     * @return name in singular form or <code>null</code> if failed to find one.
     */
    @SuppressWarnings({"HardCodedStringLiteral"})
    
    
    public static String unpluralize( final String name) {
        if (name.endsWith("sses") || name.endsWith("shes") || name.endsWith("ches") || name.endsWith("xes")) { //?
            return name.substring(0, name.length() - 2);
        }

        if (name.endsWith("ses")) {
            return name.substring(0, name.length() - 1);
        }

        if (name.endsWith("ies")) {
            if (name.endsWith("cookies") || name.endsWith("Cookies")) {
                return name.substring(0, name.length() - "ookies".length()) + "ookie";
            }

            return name.substring(0, name.length() - 3) + "y";
        }

        if (name.endsWith("leaves") || name.endsWith("Leaves")) {
            return name.substring(0, name.length() - "eaves".length()) + "eaf";
        }

        String result = stripEnding(name, "s");
        if (result != null) {
            return result;
        }

        if (name.endsWith("children")) {
            return name.substring(0, name.length() - "children".length()) + "child";
        }

        if (name.endsWith("Children") && name.length() > "Children".length()) {
            return name.substring(0, name.length() - "Children".length()) + "Child";
        }


        return null;
    }

    
    
    private static String stripEnding( String name,  String ending) {
        if (name.endsWith(ending)) {
            if (name.equals(ending)) return name; // do not return empty string
            return name.substring(0, name.length() - 1);
        }
        return null;
    }

    
    public static boolean containsAlphaCharacters( String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) return true;
        }
        return false;
    }

    
    public static boolean containsAnyChar( final String value,  final String chars) {
        if (chars.length() > value.length()) {
            return containsAnyChar(value, chars, 0, value.length());
        }
        else {
            return containsAnyChar(chars, value, 0, chars.length());
        }
    }

    
    public static boolean containsAnyChar( final String value,
                                           final String chars,
                                          final int start, final int end) {
        for (int i = start; i < end; i++) {
            if (chars.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }

        return false;
    }

    
    public static boolean containsChar( final String value, final char ch) {
        return value.indexOf(ch) >= 0;
    }

    /**
     * @deprecated use #capitalize(String)
     */
    public static String firstLetterToUpperCase( final String displayString) {
        if (displayString == null || displayString.isEmpty()) return displayString;
        char firstChar = displayString.charAt(0);
        char uppedFirstChar = toUpperCase(firstChar);

        if (uppedFirstChar == firstChar) return displayString;

        char[] buffer = displayString.toCharArray();
        buffer[0] = uppedFirstChar;
        return StringFactory.createShared(buffer);
    }

    /**
     * Strip out all characters not accepted by given filter
     *
     * @param s      e.g. "/n    my string "
     * @param filter e.g. {@link CharFilter#NOT_WHITESPACE_FILTER}
     * @return stripped string e.g. "mystring"
     */
    
    
    public static String strip( final String s,  final CharFilter filter) {
        final StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (filter.accept(ch)) {
                result.append(ch);
            }
        }
        return result.toString();
    }

    
    
    public static List<String> findMatches( String s,  Pattern pattern) {
        return findMatches(s, pattern, 1);
    }

    
    
    public static List<String> findMatches( String s,  Pattern pattern, int groupIndex) {
        List<String> result = new SmartList<String>();
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            String group = m.group(groupIndex);
            if (group != null) {
                result.add(group);
            }
        }
        return result;
    }

    /**
     * Find position of the first character accepted by given filter.
     *
     * @param s      the string to search
     * @param filter search filter
     * @return position of the first character accepted or -1 if not found
     */
    
    public static int findFirst( final CharSequence s,  CharFilter filter) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (filter.accept(ch)) {
                return i;
            }
        }
        return -1;
    }

    
    
    public static String replaceSubstring( String string,  TextRange range,  String replacement) {
        return range.replace(string, replacement);
    }

    
    public static boolean startsWithWhitespace( String text) {
        return !text.isEmpty() && Character.isWhitespace(text.charAt(0));
    }

    
    public static boolean isChar(CharSequence seq, int index, char c) {
        return index >= 0 && index < seq.length() && seq.charAt(index) == c;
    }

    
    public static boolean startsWith( CharSequence text,  CharSequence prefix) {
        int l1 = text.length();
        int l2 = prefix.length();
        if (l1 < l2) return false;

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i) != prefix.charAt(i)) return false;
        }

        return true;
    }

    
    public static boolean startsWith( CharSequence text, int startIndex,  CharSequence prefix) {
        int l1 = text.length() - startIndex;
        int l2 = prefix.length();
        if (l1 < l2) return false;

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
        }

        return true;
    }

    
    public static boolean endsWith( CharSequence text,  CharSequence suffix) {
        int l1 = text.length();
        int l2 = suffix.length();
        if (l1 < l2) return false;

        for (int i = l1 - 1; i >= l1 - l2; i--) {
            if (text.charAt(i) != suffix.charAt(i + l2 - l1)) return false;
        }

        return true;
    }

    
    
    public static String commonPrefix( String s1,  String s2) {
        return s1.substring(0, commonPrefixLength(s1, s2));
    }

    
    public static int commonPrefixLength( CharSequence s1,  CharSequence s2) {
        int i;
        int minLength = Math.min(s1.length(), s2.length());
        for (i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
        }
        return i;
    }

    
    
    public static String commonSuffix( String s1,  String s2) {
        return s1.substring(s1.length() - commonSuffixLength(s1, s2));
    }

    
    public static int commonSuffixLength( CharSequence s1,  CharSequence s2) {
        int s1Length = s1.length();
        int s2Length = s2.length();
        if (s1Length == 0 || s2Length == 0) return 0;
        int i;
        for (i = 0; i < s1Length && i < s2Length; i++) {
            if (s1.charAt(s1Length - i - 1) != s2.charAt(s2Length - i - 1)) {
                break;
            }
        }
        return i;
    }

    /**
     * Allows to answer if target symbol is contained at given char sequence at <code>[start; end)</code> interval.
     *
     * @param s     target char sequence to check
     * @param start start offset to use within the given char sequence (inclusive)
     * @param end   end offset to use within the given char sequence (exclusive)
     * @param c     target symbol to check
     * @return <code>true</code> if given symbol is contained at the target range of the given char sequence;
     * <code>false</code> otherwise
     */
    
    public static boolean contains( CharSequence s, int start, int end, char c) {
        return indexOf(s, c, start, end) >= 0;
    }

    
    public static boolean containsWhitespaces( CharSequence s) {
        if (s == null) return false;

        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return true;
        }
        return false;
    }

    
    public static int indexOf( CharSequence s, char c) {
        return indexOf(s, c, 0, s.length());
    }

    
    public static int indexOf( CharSequence s, char c, int start) {
        return indexOf(s, c, start, s.length());
    }

    
    public static int indexOf( CharSequence s, char c, int start, int end) {
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == c) return i;
        }
        return -1;
    }

    
    public static boolean contains( CharSequence sequence,  CharSequence infix) {
        return indexOf(sequence, infix) >= 0;
    }

    
    public static int indexOf( CharSequence sequence,  CharSequence infix) {
        return indexOf(sequence, infix, 0);
    }

    
    public static int indexOf( CharSequence sequence,  CharSequence infix, int start) {
        for (int i = start; i <= sequence.length() - infix.length(); i++) {
            if (startsWith(sequence, i, infix)) {
                return i;
            }
        }
        return -1;
    }

    
    public static int indexOf( CharSequence s, char c, int start, int end, boolean caseSensitive) {
        for (int i = start; i < end; i++) {
            if (charsMatch(s.charAt(i), c, !caseSensitive)) return i;
        }
        return -1;
    }

    
    public static int indexOf( char[] s, char c, int start, int end, boolean caseSensitive) {
        for (int i = start; i < end; i++) {
            if (charsMatch(s[i], c, !caseSensitive)) return i;
        }
        return -1;
    }

    
    public static int indexOfSubstringEnd( String text,  String subString) {
        int i = text.indexOf(subString);
        if (i == -1) return -1;
        return i + subString.length();
    }

    
    public static int indexOfAny( final String s,  final String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    
    public static int indexOfAny( final CharSequence s,  final String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    
    public static int indexOfAny( final String s,  final String chars, final int start, final int end) {
        return indexOfAny((CharSequence)s, chars, start, end);
    }

    
    public static int indexOfAny( final CharSequence s,  final String chars, final int start, final int end) {
        for (int i = start; i < end; i++) {
            if (containsChar(chars, s.charAt(i))) return i;
        }
        return -1;
    }

    
    
    public static String substringBefore( String text,  String subString) {
        int i = text.indexOf(subString);
        if (i == -1) return null;
        return text.substring(0, i);
    }

    
    
    public static String substringAfter( String text,  String subString) {
        int i = text.indexOf(subString);
        if (i == -1) return null;
        return text.substring(i + subString.length());
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
        return StringUtilRt.lastIndexOf(s, c, start, end);
    }

    
    
    public static String first( String text, final int maxLength, final boolean appendEllipsis) {
        return text.length() > maxLength ? text.substring(0, maxLength) + (appendEllipsis ? "..." : "") : text;
    }

    
    
    public static CharSequence first( CharSequence text, final int length, final boolean appendEllipsis) {
        return text.length() > length ? text.subSequence(0, length) + (appendEllipsis ? "..." : "") : text;
    }

    
    
    public static CharSequence last( CharSequence text, final int length, boolean prependEllipsis) {
        return text.length() > length ? (prependEllipsis ? "..." : "") + text.subSequence(text.length() - length, text.length()) : text;
    }

    
    
    public static String escapeChar( final String str, final char character) {
        return escapeChars(str, character);
    }

    
    
    public static String escapeChars( final String str, final char... character) {
        final StringBuilder buf = new StringBuilder(str);
        for (char c : character) {
            escapeChar(buf, c);
        }
        return buf.toString();
    }

    private static void escapeChar( final StringBuilder buf, final char character) {
        int idx = 0;
        while ((idx = indexOf(buf, character, idx)) >= 0) {
            buf.insert(idx, "\\");
            idx += 2;
        }
    }

    
    
    public static String escapeQuotes( final String str) {
        return escapeChar(str, '"');
    }

    public static void escapeQuotes( final StringBuilder buf) {
        escapeChar(buf, '"');
    }

    
    
    public static String escapeSlashes( final String str) {
        return escapeChar(str, '/');
    }

    
    
    public static String escapeBackSlashes( final String str) {
        return escapeChar(str, '\\');
    }

    public static void escapeSlashes( final StringBuilder buf) {
        escapeChar(buf, '/');
    }

    
    
    public static String unescapeSlashes( final String str) {
        final StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, '/');
        return buf.toString();
    }

    
    
    public static String unescapeBackSlashes( final String str) {
        final StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, '\\');
        return buf.toString();
    }

    
    
    public static String unescapeChar( final String str, char unescapeChar) {
        final StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, unescapeChar);
        return buf.toString();
    }

    private static void unescapeChar( StringBuilder buf,  String str, char unescapeChar) {
        final int length = str.length();
        final int last = length - 1;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i != last) {
                i++;
                ch = str.charAt(i);
                if (ch != unescapeChar) buf.append('\\');
            }

            buf.append(ch);
        }
    }

    public static void quote( final StringBuilder builder) {
        quote(builder, '\"');
    }

    public static void quote( final StringBuilder builder, final char quotingChar) {
        builder.insert(0, quotingChar);
        builder.append(quotingChar);
    }

    
    
    public static String wrapWithDoubleQuote( String str) {
        return '\"' + str + "\"";
    }

     private static final String[] REPLACES_REFS = {"&lt;", "&gt;", "&amp;", "&#39;", "&quot;"};
     private static final String[] REPLACES_DISP = {"<", ">", "&", "'", "\""};


    public static String unescapeXml( final String text) {
        if (text == null) return null;
        return replace(text, REPLACES_REFS, REPLACES_DISP);
    }


    public static String escapeXml( final String text) {
        if (text == null) return null;
        return replace(text, REPLACES_DISP, REPLACES_REFS);
    }

     private static final String[] MN_QUOTED = {"&&", "__"};
     private static final String[] MN_CHARS = {"&", "_"};


    public static String escapeMnemonics( String text) {
        if (text == null) return null;
        return replace(text, MN_CHARS, MN_QUOTED);
    }

    
    
    public static String htmlEmphasize( String text) {
        return "<b><code>" + escapeXml(text) + "</code></b>";
    }


    
    
    public static String escapeToRegexp( String text) {
        final StringBuilder result = new StringBuilder(text.length());
        return escapeToRegexp(text, result).toString();
    }

    
    public static StringBuilder escapeToRegexp( CharSequence text,  StringBuilder builder) {
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == ' ' || Character.isLetter(c) || Character.isDigit(c) || c == '_') {
                builder.append(c);
            }
            else if (c == '\n') {
                builder.append("\\n");
            }
            else {
                builder.append('\\').append(c);
            }
        }

        return builder;
    }

    
    public static boolean isEscapedBackslash( char[] chars, int startOffset, int backslashOffset) {
        if (chars[backslashOffset] != '\\') {
            return true;
        }
        boolean escaped = false;
        for (int i = startOffset; i < backslashOffset; i++) {
            if (chars[i] == '\\') {
                escaped = !escaped;
            }
            else {
                escaped = false;
            }
        }
        return escaped;
    }

    
    public static boolean isEscapedBackslash( CharSequence text, int startOffset, int backslashOffset) {
        if (text.charAt(backslashOffset) != '\\') {
            return true;
        }
        boolean escaped = false;
        for (int i = startOffset; i < backslashOffset; i++) {
            if (text.charAt(i) == '\\') {
                escaped = !escaped;
            }
            else {
                escaped = false;
            }
        }
        return escaped;
    }

    
    
    public static String replace( String text,  String[] from,  String[] to) {
        final StringBuilder result = new StringBuilder(text.length());
        replace:
        for (int i = 0; i < text.length(); i++) {
            for (int j = 0; j < from.length; j += 1) {
                String toReplace = from[j];
                String replaceWith = to[j];

                final int len = toReplace.length();
                if (text.regionMatches(i, toReplace, 0, len)) {
                    result.append(replaceWith);
                    i += len - 1;
                    continue replace;
                }
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    
    
    public static String[] filterEmptyStrings( String[] strings) {
        int emptyCount = 0;
        for (String string : strings) {
            if (string == null || string.isEmpty()) emptyCount++;
        }
        if (emptyCount == 0) return strings;

        String[] result = ArrayUtil.newStringArray(strings.length - emptyCount);
        int count = 0;
        for (String string : strings) {
            if (string == null || string.isEmpty()) continue;
            result[count++] = string;
        }

        return result;
    }

    
    public static int countNewLines( CharSequence text) {
        return countChars(text, '\n');
    }

    
    public static int countChars( CharSequence text, char c) {
        return countChars(text, c, 0, false);
    }

    
    public static int countChars( CharSequence text, char c, int offset, boolean continuous) {
        int count = 0;
        for (int i = offset; i < text.length(); ++i) {
            if (text.charAt(i) == c) {
                count++;
            }
            else if (continuous) {
                break;
            }
        }
        return count;
    }

    
    
    public static String capitalsOnly( String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                b.append(s.charAt(i));
            }
        }

        return b.toString();
    }

    /**
     * @param args Strings to join.
     * @return {@code null} if any of given Strings is {@code null}.
     */
    
    
    public static String joinOrNull( String... args) {
        StringBuilder r = new StringBuilder();
        for (String arg : args) {
            if (arg == null) return null;
            r.append(arg);
        }
        return r.toString();
    }

    
    
    public static String getPropertyName(  String methodName) {
        if (methodName.startsWith("get")) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        else if (methodName.startsWith("is")) {
            return Introspector.decapitalize(methodName.substring(2));
        }
        else if (methodName.startsWith("set")) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        else {
            return null;
        }
    }

    
    public static boolean isJavaIdentifierStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || Character.isJavaIdentifierStart(c);
    }

    
    public static boolean isJavaIdentifierPart(char c) {
        return c >= '0' && c <= '9' || isJavaIdentifierStart(c);
    }

    
    public static boolean isJavaIdentifier( String text) {
        int len = text.length();
        if (len == 0) return false;

        if (!isJavaIdentifierStart(text.charAt(0))) return false;

        for (int i = 1; i < len; i++) {
            if (!isJavaIdentifierPart(text.charAt(i))) return false;
        }

        return true;
    }

    /**
     * Escape property name or key in property file. Unicode characters are escaped as well.
     *
     * @param input an input to escape
     * @param isKey if true, the rules for key escaping are applied. The leading space is escaped in that case.
     * @return an escaped string
     */
    
    
    public static String escapeProperty( String input, final boolean isKey) {
        final StringBuilder escaped = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char ch = input.charAt(i);
            switch (ch) {
                case ' ':
                    if (isKey && i == 0) {
                        // only the leading space has to be escaped
                        escaped.append('\\');
                    }
                    escaped.append(' ');
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\\':
                case '#':
                case '!':
                case ':':
                case '=':
                    escaped.append('\\');
                    escaped.append(ch);
                    break;
                default:
                    if (20 < ch && ch < 0x7F) {
                        escaped.append(ch);
                    }
                    else {
                        escaped.append("\\u");
                        escaped.append(Character.forDigit((ch >> 12) & 0xF, 16));
                        escaped.append(Character.forDigit((ch >> 8) & 0xF, 16));
                        escaped.append(Character.forDigit((ch >> 4) & 0xF, 16));
                        escaped.append(Character.forDigit((ch) & 0xF, 16));
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    
    public static String getQualifiedName( String packageName, String className) {
        if (packageName == null || packageName.isEmpty()) {
            return className;
        }
        return packageName + '.' + className;
    }

    
    public static int compareVersionNumbers( String v1,  String v2) {
        // todo duplicates com.gome.maven.util.text.VersionComparatorUtil.compare
        // todo please refactor next time you make changes here
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }

        String[] part1 = v1.split("[\\.\\_\\-]");
        String[] part2 = v2.split("[\\.\\_\\-]");

        int idx = 0;
        for (; idx < part1.length && idx < part2.length; idx++) {
            String p1 = part1[idx];
            String p2 = part2[idx];

            int cmp;
            if (p1.matches("\\d+") && p2.matches("\\d+")) {
                cmp = new Integer(p1).compareTo(new Integer(p2));
            }
            else {
                cmp = part1[idx].compareTo(part2[idx]);
            }
            if (cmp != 0) return cmp;
        }

        if (part1.length == part2.length) {
            return 0;
        }
        else {
            boolean left = part1.length > idx;
            String[] parts = left ? part1 : part2;

            for (; idx < parts.length; idx++) {
                String p = parts[idx];
                int cmp;
                if (p.matches("\\d+")) {
                    cmp = new Integer(p).compareTo(0);
                }
                else {
                    cmp = 1;
                }
                if (cmp != 0) return left ? cmp : -cmp;
            }
            return 0;
        }
    }

    
    public static int getOccurrenceCount( String text, final char c) {
        int res = 0;
        int i = 0;
        while (i < text.length()) {
            i = text.indexOf(c, i);
            if (i >= 0) {
                res++;
                i++;
            }
            else {
                break;
            }
        }
        return res;
    }

    
    public static int getOccurrenceCount( String text,  String s) {
        int res = 0;
        int i = 0;
        while (i < text.length()) {
            i = text.indexOf(s, i);
            if (i >= 0) {
                res++;
                i++;
            }
            else {
                break;
            }
        }
        return res;
    }

    
    
    public static String fixVariableNameDerivedFromPropertyName( String name) {
        if (isEmptyOrSpaces(name)) return name;
        char c = name.charAt(0);
        if (isVowel(c)) {
            return "an" + Character.toUpperCase(c) + name.substring(1);
        }
        return "a" + Character.toUpperCase(c) + name.substring(1);
    }

    
    
    public static String sanitizeJavaIdentifier( String name) {
        final StringBuilder result = new StringBuilder(name.length());

        for (int i = 0; i < name.length(); i++) {
            final char ch = name.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                if (result.length() == 0 && !Character.isJavaIdentifierStart(ch)) {
                    result.append("_");
                }
                result.append(ch);
            }
        }

        return result.toString();
    }

    public static void assertValidSeparators( CharSequence s) {
        char[] chars = CharArrayUtil.fromSequenceWithoutCopying(s);
        int slashRIndex = -1;

        if (chars != null) {
            for (int i = 0, len = s.length(); i < len; ++i) {
                if (chars[i] == '\r') {
                    slashRIndex = i;
                    break;
                }
            }
        }
        else {
            for (int i = 0, len = s.length(); i < len; i++) {
                if (s.charAt(i) == '\r') {
                    slashRIndex = i;
                    break;
                }
            }
        }

        if (slashRIndex != -1) {
            String context =
                    String.valueOf(last(s.subSequence(0, slashRIndex), 10, true)) + first(s.subSequence(slashRIndex, s.length()), 10, true);
            context = escapeStringCharacters(context);
            LOG.error("Wrong line separators: '" + context + "' at offset " + slashRIndex);
        }
    }

    
    
    public static String tail( String s, final int idx) {
        return idx >= s.length() ? "" : s.substring(idx, s.length());
    }

    /**
     * Splits string by lines.
     *
     * @param string String to split
     * @return array of strings
     */
    
    
    public static String[] splitByLines( String string) {
        return splitByLines(string, true);
    }

    /**
     * Splits string by lines. If several line separators are in a row corresponding empty lines
     * are also added to result if {@code excludeEmptyStrings} is {@code false}.
     *
     * @param string String to split
     * @return array of strings
     */
    
    
    public static String[] splitByLines( String string, boolean excludeEmptyStrings) {
        return (excludeEmptyStrings ? EOL_SPLIT_PATTERN : EOL_SPLIT_PATTERN_WITH_EMPTY).split(string);
    }

    
    
    public static String[] splitByLinesDontTrim( String string) {
        return EOL_SPLIT_DONT_TRIM_PATTERN.split(string);
    }

    /**
     * Splits string by lines, keeping all line separators at the line ends and in the empty lines.
     * <br> E.g. splitting text
     * <blockquote>
     *   foo\r\n<br>
     *   \n<br>
     *   bar\n<br>
     *   \r\n<br>
     *   baz\r<br>
     *   \r<br>
     * </blockquote>
     * will return the following array: foo\r\n, \n, bar\n, \r\n, baz\r, \r
     *
     */
    
    
    public static String[] splitByLinesKeepSeparators( String string) {
        return EOL_SPLIT_KEEP_SEPARATORS.split(string);
    }

    
    
    public static List<Pair<String, Integer>> getWordsWithOffset(String s) {
        List<Pair<String, Integer>> res = ContainerUtil.newArrayList();
        s += " ";
        StringBuilder name = new StringBuilder();
        int startInd = -1;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                if (name.length() > 0) {
                    res.add(Pair.create(name.toString(), startInd));
                    name.setLength(0);
                    startInd = -1;
                }
            }
            else {
                if (startInd == -1) {
                    startInd = i;
                }
                name.append(s.charAt(i));
            }
        }
        return res;
    }

    /**
     * Implementation of "Sorting for Humans: Natural Sort Order":
     * http://www.codinghorror.com/blog/2007/12/sorting-for-humans-natural-sort-order.html
     */
    
    public static int naturalCompare( String string1,  String string2) {
        return naturalCompare(string1, string2, false);
    }

    
    private static int naturalCompare( String string1,  String string2, boolean caseSensitive) {
        //noinspection StringEquality
        if (string1 == string2) {
            return 0;
        }
        if (string1 == null) {
            return -1;
        }
        if (string2 == null) {
            return 1;
        }

        final int string1Length = string1.length();
        final int string2Length = string2.length();
        int i = 0;
        int j = 0;
        for (; i < string1Length && j < string2Length; i++, j++) {
            char ch1 = string1.charAt(i);
            char ch2 = string2.charAt(j);
            if ((isDecimalDigit(ch1) || ch1 == ' ') && (isDecimalDigit(ch2) || ch2 == ' ')) {
                int startNum1 = i;
                while (ch1 == ' ' || ch1 == '0') { // skip leading spaces and zeros
                    startNum1++;
                    if (startNum1 >= string1Length) break;
                    ch1 = string1.charAt(startNum1);
                }
                int startNum2 = j;
                while (ch2 == ' ' || ch2 == '0') { // skip leading spaces and zeros
                    startNum2++;
                    if (startNum2 >= string2Length) break;
                    ch2 = string2.charAt(startNum2);
                }
                i = startNum1;
                j = startNum2;
                // find end index of number
                while (i < string1Length && isDecimalDigit(string1.charAt(i))) i++;
                while (j < string2Length && isDecimalDigit(string2.charAt(j))) j++;
                final int lengthDiff = (i - startNum1) - (j - startNum2);
                if (lengthDiff != 0) {
                    // numbers with more digits are always greater than shorter numbers
                    return lengthDiff;
                }
                for (; startNum1 < i; startNum1++, startNum2++) {
                    // compare numbers with equal digit count
                    final int diff = string1.charAt(startNum1) - string2.charAt(startNum2);
                    if (diff != 0) {
                        return diff;
                    }
                }
                i--;
                j--;
            }
            else {
                if (caseSensitive) {
                    return ch1 - ch2;
                }
                else {
                    // similar logic to charsMatch() below
                    if (ch1 != ch2) {
                        final int diff1 = StringUtilRt.toUpperCase(ch1) - StringUtilRt.toUpperCase(ch2);
                        if (diff1 != 0) {
                            final int diff2 = StringUtilRt.toLowerCase(ch1) - StringUtilRt.toLowerCase(ch2);
                            if (diff2 != 0) {
                                return diff2;
                            }
                        }
                    }
                }
            }
        }
        // After the loop the end of one of the strings might not have been reached, if the other
        // string ends with a number and the strings are equal until the end of that number. When
        // there are more characters in the string, then it is greater.
        if (i < string1Length) {
            return 1;
        }
        else if (j < string2Length) {
            return -1;
        }
        if (!caseSensitive && string1Length == string2Length) {
            // do case sensitive compare if case insensitive strings are equal
            return naturalCompare(string1, string2, true);
        }
        return string1Length - string2Length;
    }

    
    public static boolean isDecimalDigit(char c) {
        return c >= '0' && c <= '9';
    }

    
    public static int compare( String s1,  String s2, boolean ignoreCase) {
        //noinspection StringEquality
        if (s1 == s2) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return ignoreCase ? s1.compareToIgnoreCase(s2) : s1.compareTo(s2);
    }

    
    public static int comparePairs( String s1,  String t1,  String s2,  String t2, boolean ignoreCase) {
        final int compare = compare(s1, s2, ignoreCase);
        return compare != 0 ? compare : compare(t1, t2, ignoreCase);
    }

    
    public static int hashCode( CharSequence s) {
        return stringHashCode(s);
    }

    
    public static boolean equals( CharSequence s1,  CharSequence s2) {
        if (s1 == null ^ s2 == null) {
            return false;
        }

        if (s1 == null) {
            return true;
        }

        if (s1.length() != s2.length()) {
            return false;
        }
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    
    public static boolean equalsIgnoreCase( CharSequence s1,  CharSequence s2) {
        if (s1 == null ^ s2 == null) {
            return false;
        }

        if (s1 == null) {
            return true;
        }

        if (s1.length() != s2.length()) {
            return false;
        }
        for (int i = 0; i < s1.length(); i++) {
            if (!charsMatch(s1.charAt(i), s2.charAt(i), true)) {
                return false;
            }
        }
        return true;
    }

    
    public static boolean equalsIgnoreWhitespaces( CharSequence s1,  CharSequence s2) {
        if (s1 == null ^ s2 == null) {
            return false;
        }

        if (s1 == null) {
            return true;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        int index1 = 0;
        int index2 = 0;
        while (index1 < len1 && index2 < len2) {
            if (s1.charAt(index1) == s2.charAt(index2)) {
                index1++;
                index2++;
                continue;
            }

            boolean skipped = false;
            while (index1 != len1 && isWhiteSpace(s1.charAt(index1))) {
                skipped = true;
                index1++;
            }
            while (index2 != len2 && isWhiteSpace(s2.charAt(index2))) {
                skipped = true;
                index2++;
            }

            if (!skipped) return false;
        }

        for (; index1 != len1; index1++) {
            if (!isWhiteSpace(s1.charAt(index1))) return false;
        }
        for (; index2 != len2; index2++) {
            if (!isWhiteSpace(s2.charAt(index2))) return false;
        }

        return true;
    }

    
    public static boolean equalsTrimWhitespaces( CharSequence s1,  CharSequence s2) {
        int start1 = 0;
        int end1 = s1.length();
        int start2 = 0;
        int end2 = s2.length();

        while (start1 < end1) {
            char c = s1.charAt(start1);
            if (!isWhiteSpace(c)) break;
            start1++;
        }

        while (start1 < end1) {
            char c = s1.charAt(end1 - 1);
            if (!isWhiteSpace(c)) break;
            end1--;
        }

        while (start2 < end2) {
            char c = s2.charAt(start2);
            if (!isWhiteSpace(c)) break;
            start2++;
        }

        while (start2 < end2) {
            char c = s2.charAt(end2 - 1);
            if (!isWhiteSpace(c)) break;
            end2--;
        }

        CharSequence ts1 = new CharSequenceSubSequence(s1, start1, end1);
        CharSequence ts2 = new CharSequenceSubSequence(s2, start2, end2);

        return equals(ts1, ts2);
    }

    
    public static int compare(char c1, char c2, boolean ignoreCase) {
        // duplicating String.equalsIgnoreCase logic
        int d = c1 - c2;
        if (d == 0 || !ignoreCase) {
            return d;
        }
        // If characters don't match but case may be ignored,
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should
        // continue.
        char u1 = StringUtilRt.toUpperCase(c1);
        char u2 = StringUtilRt.toUpperCase(c2);
        d = u1 - u2;
        if (d != 0) {
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before
            // exiting.
            d = StringUtilRt.toLowerCase(u1) - StringUtilRt.toLowerCase(u2);
        }
        return d;
    }

    
    public static boolean charsMatch(char c1, char c2, boolean ignoreCase) {
        return compare(c1, c2, ignoreCase) == 0;
    }

    
    
    public static String formatLinks( String message) {
        Pattern linkPattern = Pattern.compile("http://[a-zA-Z0-9\\./\\-\\+]+");
        StringBuffer result = new StringBuffer();
        Matcher m = linkPattern.matcher(message);
        while (m.find()) {
            m.appendReplacement(result, "<a href=\"" + m.group() + "\">" + m.group() + "</a>");
        }
        m.appendTail(result);
        return result.toString();
    }

    
    public static boolean isHexDigit(char c) {
        return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
    }

    
    public static boolean isOctalDigit(char c) {
        return '0' <= c && c <= '7';
    }

    
    
    public static String shortenTextWithEllipsis( final String text, final int maxLength, final int suffixLength) {
        return shortenTextWithEllipsis(text, maxLength, suffixLength, false);
    }

    
    
    public static String trimMiddle( String text, int maxLength) {
        return shortenTextWithEllipsis(text, maxLength, maxLength >> 1, true);
    }

    
    
    public static String shortenTextWithEllipsis( final String text,
                                                 final int maxLength,
                                                 final int suffixLength,
                                                  String symbol) {
        final int textLength = text.length();
        if (textLength > maxLength) {
            final int prefixLength = maxLength - suffixLength - symbol.length();
            assert prefixLength > 0;
            return text.substring(0, prefixLength) + symbol + text.substring(textLength - suffixLength);
        }
        else {
            return text;
        }
    }

    
    
    public static String shortenTextWithEllipsis( final String text,
                                                 final int maxLength,
                                                 final int suffixLength,
                                                 boolean useEllipsisSymbol) {
        String symbol = useEllipsisSymbol ? "\u2026" : "...";
        return shortenTextWithEllipsis(text, maxLength, suffixLength, symbol);
    }

    
    
    public static String shortenPathWithEllipsis( final String path, final int maxLength, boolean useEllipsisSymbol) {
        return shortenTextWithEllipsis(path, maxLength, (int)(maxLength * 0.7), useEllipsisSymbol);
    }

    
    
    public static String shortenPathWithEllipsis( final String path, final int maxLength) {
        return shortenPathWithEllipsis(path, maxLength, false);
    }

    
    public static boolean charsEqual(char a, char b, boolean ignoreCase) {
        return ignoreCase ? charsEqualIgnoreCase(a, b) : a == b;
    }

    
    public static boolean charsEqualIgnoreCase(char a, char b) {
        return StringUtilRt.charsEqualIgnoreCase(a, b);
    }

    
    public static char toUpperCase(char a) {
        return StringUtilRt.toUpperCase(a);
    }

    
    
    public static String toUpperCase( String a) {
        return StringUtilRt.toUpperCase(a).toString();
    }

    
    public static char toLowerCase(final char a) {
        return StringUtilRt.toLowerCase(a);
    }

    
    public static LineSeparator detectSeparators(CharSequence text) {
        int index = indexOfAny(text, "\n\r");
        if (index == -1) return null;
        if (startsWith(text, index, "\r\n")) return LineSeparator.CRLF;
        if (text.charAt(index) == '\r') return LineSeparator.CR;
        if (text.charAt(index) == '\n') return LineSeparator.LF;
        throw new IllegalStateException();
    }

    
    
    public static String convertLineSeparators( String text) {
        return StringUtilRt.convertLineSeparators(text);
    }

    
    
    public static String convertLineSeparators( String text, boolean keepCarriageReturn) {
        return StringUtilRt.convertLineSeparators(text, keepCarriageReturn);
    }

    
    
    public static String convertLineSeparators( String text,  String newSeparator) {
        return StringUtilRt.convertLineSeparators(text, newSeparator);
    }

    
    public static String convertLineSeparators( String text,  String newSeparator,  int[] offsetsToKeep) {
        return StringUtilRt.convertLineSeparators(text, newSeparator, offsetsToKeep);
    }

    
    public static String convertLineSeparators( String text,
                                                String newSeparator,
                                                int[] offsetsToKeep,
                                               boolean keepCarriageReturn) {
        return StringUtilRt.convertLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn);
    }

    
    public static int parseInt(final String string, final int defaultValue) {
        return StringUtilRt.parseInt(string, defaultValue);
    }

    
    public static double parseDouble(final String string, final double defaultValue) {
        return StringUtilRt.parseDouble(string, defaultValue);
    }

    
    public static boolean parseBoolean(String string, final boolean defaultValue) {
        return StringUtilRt.parseBoolean(string, defaultValue);
    }

    
    
    public static String getShortName( Class aClass) {
        return StringUtilRt.getShortName(aClass);
    }

    
    
    public static String getShortName( String fqName) {
        return StringUtilRt.getShortName(fqName);
    }

    
    
    public static String getShortName( String fqName, char separator) {
        return StringUtilRt.getShortName(fqName, separator);
    }

    
    
    public static CharSequence newBombedCharSequence( CharSequence sequence, long delay) {
        final long myTime = System.currentTimeMillis() + delay;
        return new BombedCharSequence(sequence) {
            @Override
            protected void checkCanceled() {
                long l = System.currentTimeMillis();
                if (l >= myTime) {
                    throw new ProcessCanceledException();
                }
            }
        };
    }

    public static boolean trimEnd( StringBuilder buffer,  CharSequence end) {
        if (endsWith(buffer, end)) {
            buffer.delete(buffer.length() - end.length(), buffer.length());
            return true;
        }
        return false;
    }

    /**
     * Say smallPart = "op" and bigPart="open". Method returns true for "Ope" and false for "ops"
     */
    
    public static boolean isBetween( String string,  String smallPart,  String bigPart) {
        final String s = string.toLowerCase();
        return s.startsWith(smallPart.toLowerCase()) && bigPart.toLowerCase().startsWith(s);
    }

    
    public static String getShortened( String s, int maxWidth) {
        int length = s.length();
        if (isEmpty(s) || length <= maxWidth) return s;
        ArrayList<String> words = new ArrayList<String>();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);

            if (i == length - 1) {
                builder.append(ch);
                words.add(builder.toString());
                builder.delete(0, builder.length());
                continue;
            }

            if (i > 0 && (ch == '/' || ch == '\\' || ch == '.' || ch == '-' || Character.isUpperCase(ch))) {
                words.add(builder.toString());
                builder.delete(0, builder.length());
            }
            builder.append(ch);
        }
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (i < words.size() - 1 && word.length() == 1) {
                words.remove(i);
                words.set(i, word + words.get(i));
            }
        }

        int removedLength = 0;

        String toPaste = "...";
        int index;
        while (true) {
            index = Math.max(0, (words.size() - 1) / 2);
            String aWord = words.get(index);
            words.remove(index);
            int toCut = length - removedLength - maxWidth + 3;
            if (words.size() < 2 || (toCut < aWord.length() - 2 && removedLength == 0)) {
                int pos = (aWord.length() - toCut) / 2;
                toPaste = aWord.substring(0, pos) + "..." + aWord.substring(pos+toCut);
                break;
            }
            removedLength += aWord.length();
            if (length - removedLength <= maxWidth - 3) {
                break;
            }
        }
        for (int i = 0; i < Math.max(1, words.size()); i++) {
            String word = words.isEmpty() ? "" : words.get(i);
            if (i == index || words.size() == 1) builder.append(toPaste);
            builder.append(word);
        }
        return builder.toString().replaceAll("\\.{4,}", "...");
    }

    /**
     * Does the string have an uppercase character?
     * @param s  the string to test.
     * @return   true if the string has an uppercase character, false if not.
     */
    public static boolean hasUpperCaseChar(String s) {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does the string have a lowercase character?
     * @param s  the string to test.
     * @return   true if the string has a lowercase character, false if not.
     */
    public static boolean hasLowerCaseChar(String s) {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }


    private static final Pattern UNICODE_CHAR = Pattern.compile("\\\\u[0-9a-eA-E]{4}");

    public static String replaceUnicodeEscapeSequences(String text) {
        if (text == null) return null;

        final Matcher matcher = UNICODE_CHAR.matcher(text);
        if (!matcher.find()) return text; // fast path

        matcher.reset();
        int lastEnd = 0;
        final StringBuilder sb = new StringBuilder(text.length());
        while (matcher.find()) {
            sb.append(text.substring(lastEnd, matcher.start()));
            final char c = (char)Integer.parseInt(matcher.group().substring(2), 16);
            sb.append(c);
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd, text.length()));
        return sb.toString();
    }

    /**
     * Expirable CharSequence. Very useful to control external library execution time,
     * i.e. when java.util.regex.Pattern match goes out of control.
     */
    public abstract static class BombedCharSequence implements CharSequence {
        private final CharSequence delegate;
        private int i = 0;

        public BombedCharSequence( CharSequence sequence) {
            delegate = sequence;
        }

        @Override
        public int length() {
            check();
            return delegate.length();
        }

        @Override
        public char charAt(int i) {
            check();
            return delegate.charAt(i);
        }

        protected void check() {
            if ((++i & 1023) == 0) {
                checkCanceled();
            }
        }

        
        @Override
        public String toString() {
            check();
            return delegate.toString();
        }

        protected abstract void checkCanceled();

        @Override
        public CharSequence subSequence(int i, int i1) {
            check();
            return delegate.subSequence(i, i1);
        }
    }
}
