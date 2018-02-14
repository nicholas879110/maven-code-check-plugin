package com.gome.maven.openapi.util.io;

import java.io.IOException;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:59
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class FileTooBigException extends IOException {
    public FileTooBigException(String e) {
        super(e);
    }
}
