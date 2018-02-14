package com.gome.maven.openapi.diagnostic;

import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.Base64Converter;
import com.gome.maven.util.PathUtilRt;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:07
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class Attachment {
    public static final Attachment[] EMPTY_ARRAY = new Attachment[0];
    private final String myPath;
    private final byte[] myBytes;
    private boolean myIncluded = true;
    private final String myDisplayText;

    public Attachment(String path, String content) {
        myPath = path;
        myDisplayText = content;
        myBytes = getBytes(content);
    }

    public Attachment(String path, byte[] bytes, String displayText) {
        myPath = path;
        myBytes = bytes;
        myDisplayText = displayText;
    }


    public static byte[] getBytes(String content) {
        return content.getBytes(CharsetToolkit.UTF8_CHARSET);
    }


    public String getDisplayText() {
        return myDisplayText;
    }


    public String getPath() {
        return myPath;
    }


    public String getName() {
        return PathUtilRt.getFileName(myPath);
    }


    public String getEncodedBytes() {
        return Base64Converter.encode(myBytes);
    }

    public boolean isIncluded() {
        return myIncluded;
    }

    public void setIncluded(boolean included) {
        myIncluded = included;
    }
}
