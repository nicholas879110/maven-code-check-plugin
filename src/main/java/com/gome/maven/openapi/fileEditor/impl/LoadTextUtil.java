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
package com.gome.maven.openapi.fileEditor.impl;

import com.gome.maven.lang.properties.charset.Native2AsciiCharset;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationUtil;
import com.gome.maven.openapi.fileTypes.BinaryFileDecompiler;
import com.gome.maven.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.gome.maven.openapi.fileTypes.CharsetUtil;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;
import com.gome.maven.openapi.vfs.encoding.EncodingRegistry;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.ExceptionUtil;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.text.CharArrayUtil;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.Callable;

public final class LoadTextUtil {
     private static final String AUTO_DETECTED_FROM_BOM = "auto-detected from BOM";

    private LoadTextUtil() {
    }

    
    private static Pair<CharSequence, String> convertLineSeparators( CharBuffer buffer) {
        int dst = 0;
        char prev = ' ';
        int crCount = 0;
        int lfCount = 0;
        int crlfCount = 0;

        final int length = buffer.length();
        final char[] bufferArray = CharArrayUtil.fromSequenceWithoutCopying(buffer);

        for (int src = 0; src < length; src++) {
            char c = bufferArray != null ? bufferArray[src]:buffer.charAt(src);
            switch (c) {
                case '\r':
                    if(bufferArray != null) bufferArray[dst++] = '\n';
                    else buffer.put(dst++, '\n');
                    crCount++;
                    break;
                case '\n':
                    if (prev == '\r') {
                        crCount--;
                        crlfCount++;
                    }
                    else {
                        if(bufferArray != null) bufferArray[dst++] = '\n';
                        else buffer.put(dst++, '\n');
                        lfCount++;
                    }
                    break;
                default:
                    if(bufferArray != null) bufferArray[dst++] = c;
                    else buffer.put(dst++, c);
                    break;
            }
            prev = c;
        }

        String detectedLineSeparator = null;
        if (crlfCount > crCount && crlfCount > lfCount) {
            detectedLineSeparator = "\r\n";
        }
        else if (crCount > lfCount) {
            detectedLineSeparator = "\r";
        }
        else if (lfCount > 0) {
            detectedLineSeparator = "\n";
        }

        CharSequence result;
        if (buffer.length() == dst) {
            result = buffer;
        }
        else {
            // in Mac JDK CharBuffer.subSequence() signature differs from Oracle's
            // more than that, the signature has changed between jd6 and jdk7,
            // so use more generic CharSequence.subSequence() just in case
            @SuppressWarnings("UnnecessaryLocalVariable") CharSequence seq = buffer;
            result = seq.subSequence(0, dst);
        }
        return Pair.create(result, detectedLineSeparator);
    }

    
    public static Charset detectCharset( VirtualFile virtualFile,  byte[] content,  FileType fileType) {
        Charset charset = null;

        Trinity<Charset,CharsetToolkit.GuessedEncoding, byte[]> guessed = guessFromContent(virtualFile, content, content.length);
        if (guessed != null && guessed.first != null) {
            charset = guessed.first;
        }
        else {
            String charsetName = fileType.getCharset(virtualFile, content);

            if (charsetName == null) {
                Charset specifiedExplicitly = EncodingRegistry.getInstance().getEncoding(virtualFile, true);
                if (specifiedExplicitly != null) {
                    charset = specifiedExplicitly;
                }
            }
            else {
                charset = CharsetToolkit.forName(charsetName);
            }
        }

        if (charset == null) {
            charset = EncodingRegistry.getInstance().getDefaultCharset();
        }
        if (fileType.getName().equals("Properties") && EncodingRegistry.getInstance().isNative2Ascii(virtualFile)) {
            charset = Native2AsciiCharset.wrap(charset);
        }
        virtualFile.setCharset(charset);
        return charset;
    }

    
    public static Charset detectCharsetAndSetBOM( VirtualFile virtualFile,  byte[] content) {
        return doDetectCharsetAndSetBOM(virtualFile, content, true).getFirst();
    }

    
    private static Pair.NonNull<Charset, byte[]> doDetectCharsetAndSetBOM( VirtualFile virtualFile,  byte[] content, boolean saveBOM) {
        return doDetectCharsetAndSetBOM(virtualFile, content, saveBOM, virtualFile.getFileType());
    }
    
    private static Pair.NonNull<Charset, byte[]> doDetectCharsetAndSetBOM( VirtualFile virtualFile,  byte[] content, boolean saveBOM,  FileType fileType) {
         Charset charset = virtualFile.isCharsetSet() ? virtualFile.getCharset() : detectCharset(virtualFile, content,fileType);
        Pair.NonNull<Charset, byte[]> bomAndCharset = getCharsetAndBOM(content, charset);
        final byte[] bom = bomAndCharset.second;
        if (saveBOM && bom.length != 0) {
            virtualFile.setBOM(bom);
            setCharsetWasDetectedFromBytes(virtualFile, AUTO_DETECTED_FROM_BOM);
        }
        return bomAndCharset;
    }

    private static final boolean GUESS_UTF = Boolean.parseBoolean(System.getProperty("idea.guess.utf.encoding", "true"));

    public static Trinity<Charset, CharsetToolkit.GuessedEncoding, byte[]> guessFromContent( VirtualFile virtualFile,  byte[] content, int length) {
        Charset defaultCharset = ObjectUtils.notNull(EncodingManager.getInstance().getEncoding(virtualFile, true), CharsetToolkit.getDefaultSystemCharset());
        CharsetToolkit toolkit = GUESS_UTF ? new CharsetToolkit(content, defaultCharset) : null;
        String detectedFromBytes = null;
        try {
            if (GUESS_UTF) {
                toolkit.setEnforce8Bit(true);
                Charset charset = toolkit.guessFromBOM();
                if (charset != null) {
                    detectedFromBytes = AUTO_DETECTED_FROM_BOM;
                    byte[] bom = ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(charset), CharsetToolkit.UTF8_BOM);
                    return Trinity.create(charset, null, bom);
                }
                CharsetToolkit.GuessedEncoding guessed = toolkit.guessFromContent(length);
                if (guessed == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
                    detectedFromBytes = "auto-detected from bytes";
                    return Trinity.create(CharsetToolkit.UTF8_CHARSET, guessed, null); //UTF detected, ignore all directives
                }
                if (guessed == CharsetToolkit.GuessedEncoding.SEVEN_BIT) {
                    return Trinity.create(null, guessed, null);
                }
            }
            return null;
        }
        finally {
            setCharsetWasDetectedFromBytes(virtualFile, detectedFromBytes);
        }
    }

    
    private static Pair.NonNull<Charset,byte[]> getCharsetAndBOM( byte[] content,  Charset charset) {
        if (charset.name().contains(CharsetToolkit.UTF8) && CharsetToolkit.hasUTF8Bom(content)) {
            return Pair.createNonNull(charset, CharsetToolkit.UTF8_BOM);
        }
        try {
            Charset fromBOM = CharsetToolkit.guessFromBOM(content);
            if (fromBOM != null) {
                return Pair.createNonNull(fromBOM, ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(fromBOM), ArrayUtil.EMPTY_BYTE_ARRAY));
            }
        }
        catch (UnsupportedCharsetException ignore) {
        }

        return Pair.createNonNull(charset, ArrayUtil.EMPTY_BYTE_ARRAY);
    }

    public static void changeLineSeparators( Project project,
                                             VirtualFile file,
                                             String newSeparator,
                                             Object requestor) throws IOException
    {
        CharSequence currentText = getTextByBinaryPresentation(file.contentsToByteArray(), file, true, false);
        String currentSeparator = detectLineSeparator(file, false);
        if (newSeparator.equals(currentSeparator)) {
            return;
        }
        String newText = StringUtil.convertLineSeparators(currentText.toString(), newSeparator);

        file.setDetectedLineSeparator(newSeparator);
        write(project, file, requestor, newText, -1);
    }

    /**
     * Overwrites file with text and sets modification stamp and time stamp to the specified values.
     * <p/>
     * Normally you should not use this method.
     *
     * @param requestor            any object to control who called this method. Note that
     *                             it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                             See {@link com.gome.maven.openapi.vfs.VirtualFileEvent#getRequestor}
     * @param newModificationStamp new modification stamp or -1 if no special value should be set @return <code>Writer</code>
     * @throws java.io.IOException if an I/O error occurs
     * @see VirtualFile#getModificationStamp()
     */
    public static void write( Project project,
                              VirtualFile virtualFile,
                              Object requestor,
                              String text,
                             long newModificationStamp) throws IOException {
        Charset existing = virtualFile.getCharset();
        Pair.NonNull<Charset, byte[]> chosen = charsetForWriting(project, virtualFile, text, existing);
        Charset charset = chosen.first;
        byte[] buffer = chosen.second;
        if (!charset.equals(existing)) {
            virtualFile.setCharset(charset);
        }
        setDetectedFromBytesFlagBack(virtualFile, buffer);

        OutputStream outputStream = virtualFile.getOutputStream(requestor, newModificationStamp, -1);
        try {
            outputStream.write(buffer);
        }
        finally {
            outputStream.close();
        }
    }

    
    private static Pair.NonNull<Charset, byte[]> charsetForWriting( Project project,
                                                                    VirtualFile virtualFile,
                                                                    String text,
                                                                    Charset existing) {
        Charset specified = extractCharsetFromFileContent(project, virtualFile, text);
        Pair.NonNull<Charset, byte[]> chosen = chooseMostlyHarmlessCharset(existing, specified, text);
        Charset charset = chosen.first;

        // in case of "UTF-16", OutputStreamWriter sometimes adds BOM on it's own.
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6800103
        byte[] bom = virtualFile.getBOM();
        Charset fromBom = bom == null ? null : CharsetToolkit.guessFromBOM(bom);
        if (fromBom != null && !fromBom.equals(charset)) {
            chosen = Pair.createNonNull(fromBom, toBytes(text, fromBom));
        }
        return chosen;
    }

    public static void setDetectedFromBytesFlagBack( VirtualFile virtualFile,  byte[] content) {
        if (virtualFile.getBOM() == null) {
            guessFromContent(virtualFile, content, content.length);
        }
        else {
            // prevent file to be reloaded in other encoding after save with BOM
            setCharsetWasDetectedFromBytes(virtualFile, AUTO_DETECTED_FROM_BOM);
        }
    }

    
    public static Pair.NonNull<Charset, byte[]> chooseMostlyHarmlessCharset( Charset existing,  Charset specified,  String text) {
        try {
            if (specified.equals(existing)) {
                return Pair.createNonNull(specified, toBytes(text, existing));
            }

            byte[] out = isSupported(specified, text);
            if (out != null) {
                return Pair.createNonNull(specified, out); //if explicitly specified encoding is safe, return it
            }
            out = isSupported(existing, text);
            if (out != null) {
                return Pair.createNonNull(existing, out);   //otherwise stick to the old encoding if it's ok
            }
            return Pair.createNonNull(specified, toBytes(text, specified)); //if both are bad there is no difference
        }
        catch (RuntimeException e) {
            return Pair.createNonNull(Charset.defaultCharset(), toBytes(text, null)); //if both are bad and there is no hope, use the default charset
        }
    }

    
    private static byte[] toBytes( String text,  Charset charset) throws RuntimeException {
        //noinspection SSBasedInspection
        return charset == null ? text.getBytes() : text.getBytes(charset);
    }

    private static byte[] isSupported( Charset charset,  String str) {
        try {
            if (!charset.canEncode()) return null;
            byte[] bytes = str.getBytes(charset);
            if (!str.equals(new String(bytes, charset))) {
                return null;
            }

            return bytes;
        }
        catch (Exception e) {
            return null;//wow, some charsets throw NPE inside .getBytes() when unable to encode (JIS_X0212-1990)
        }
    }

    
    public static Charset extractCharsetFromFileContent( Project project,  VirtualFile virtualFile,  CharSequence text) {
        return ObjectUtils.notNull(charsetFromContentOrNull(project, virtualFile, text), virtualFile.getCharset());
    }

    public static Charset charsetFromContentOrNull( Project project,  VirtualFile virtualFile,  CharSequence text) {
        return CharsetUtil.extractCharsetFromFileContent(project, virtualFile, virtualFile.getFileType(), text);
    }

    
    public static CharSequence loadText( final VirtualFile file) {
        if (file instanceof LightVirtualFile) {
            return ((LightVirtualFile)file).getContent();
        }

        if (file.isDirectory()) {
            throw new AssertionError("'" + file.getPresentableUrl() + "' is a directory");
        }

        FileType fileType = file.getFileType();
        if (fileType.isBinary()) {
            final BinaryFileDecompiler decompiler = BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType);
            if (decompiler != null) {
                CharSequence text;

                Application app = ApplicationManager.getApplication();
                if (app != null && app.isDispatchThread() && !app.isWriteAccessAllowed() && !GraphicsEnvironment.isHeadless()) {
                    final Ref<CharSequence> result = Ref.create(ArrayUtil.EMPTY_CHAR_SEQUENCE);
                    final Ref<Throwable> error = Ref.create();
                    ProgressManager.getInstance().run(new Task.Modal(null, "Decompiling " + file.getName(), true) {
                        @Override
                        public void run( ProgressIndicator indicator) {
                            indicator.setIndeterminate(true);
                            try {
                                result.set(ApplicationUtil.runWithCheckCanceled(new Callable<CharSequence>() {
                                    @Override
                                    public CharSequence call() {
                                        return decompiler.decompile(file);
                                    }
                                }, indicator));
                            }
                            catch (Throwable t) {
                                error.set(t);
                            }
                        }
                    });
                    ExceptionUtil.rethrowUnchecked(error.get());
                    text = result.get();
                }
                else {
                    text = decompiler.decompile(file);
                }

                StringUtil.assertValidSeparators(text);
                return text;
            }

            throw new IllegalArgumentException("Attempt to load text for binary file which doesn't have a decompiler plugged in: " + file.getPresentableUrl());
        }

        try {
            byte[] bytes = file.contentsToByteArray();
            return getTextByBinaryPresentation(bytes, file);
        }
        catch (IOException e) {
            return ArrayUtil.EMPTY_CHAR_SEQUENCE;
        }
    }

    
    public static CharSequence getTextByBinaryPresentation( final byte[] bytes,  VirtualFile virtualFile) {
        return getTextByBinaryPresentation(bytes, virtualFile, true, true);
    }

    
    public static CharSequence getTextByBinaryPresentation( byte[] bytes,
                                                            VirtualFile virtualFile,
                                                           boolean saveDetectedSeparators,
                                                           boolean saveBOM) {
        return getTextByBinaryPresentation(bytes, virtualFile, saveDetectedSeparators, saveBOM, virtualFile.getFileType());
    }
    
    public static CharSequence getTextByBinaryPresentation( byte[] bytes,
                                                            VirtualFile virtualFile,
                                                           boolean saveDetectedSeparators,
                                                           boolean saveBOM,
                                                            FileType fileType) {
        Pair.NonNull<Charset, byte[]> pair = doDetectCharsetAndSetBOM(virtualFile, bytes, saveBOM, fileType);
        Charset charset = pair.getFirst();
        byte[] bom = pair.getSecond();
        int offset = bom.length;

        Pair<CharSequence, String> result = convertBytes(bytes, charset, offset);
        if (saveDetectedSeparators) {
            virtualFile.setDetectedLineSeparator(result.getSecond());
        }
        return result.getFirst();
    }

    /**
     * Get detected line separator, if the file never been loaded, is loaded if checkFile parameter is specified.
     *
     * @param file      the file to check
     * @param checkFile if the line separator was not detected before, try to detect it
     * @return the detected line separator or null
     */
    
    public static String detectLineSeparator( VirtualFile file, boolean checkFile) {
        String lineSeparator = getDetectedLineSeparator(file);
        if (lineSeparator == null && checkFile) {
            try {
                getTextByBinaryPresentation(file.contentsToByteArray(), file);
                lineSeparator = getDetectedLineSeparator(file);
            }
            catch (IOException e) {
                // null will be returned
            }
        }
        return lineSeparator;
    }

    static String getDetectedLineSeparator( VirtualFile file) {
        return file.getDetectedLineSeparator();
    }

    
    public static CharSequence getTextByBinaryPresentation( byte[] bytes,  Charset charset) {
        Pair.NonNull<Charset, byte[]> pair = getCharsetAndBOM(bytes, charset);
        byte[] bom = pair.getSecond();
        int offset = bom.length;

        final Pair<CharSequence, String> result = convertBytes(bytes, pair.first, offset);
        return result.getFirst();
    }

    // do not need to think about BOM here. it is processed outside
    
    private static Pair<CharSequence, String> convertBytes( byte[] bytes,  Charset charset, final int startOffset) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, startOffset, bytes.length - startOffset);

        CharBuffer charBuffer;
        try {
            charBuffer = charset.decode(byteBuffer);
        }
        catch (Exception e) {
            // esoteric charsets can throw any kind of exception
            charBuffer = CharBuffer.wrap(ArrayUtil.EMPTY_CHAR_ARRAY);
        }
        return convertLineSeparators(charBuffer);
    }

    private static final Key<String> CHARSET_WAS_DETECTED_FROM_BYTES = Key.create("CHARSET_WAS_DETECTED_FROM_BYTES");

    public static String wasCharsetDetectedFromBytes( VirtualFile virtualFile) {
        return virtualFile.getUserData(CHARSET_WAS_DETECTED_FROM_BYTES);
    }

    public static void setCharsetWasDetectedFromBytes( VirtualFile virtualFile,
                                                       String reason) {
        virtualFile.putUserData(CHARSET_WAS_DETECTED_FROM_BYTES, reason);
    }
}
