package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 17:00
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class InvalidDataException extends Exception {
    public InvalidDataException() {
        super();
    }

    public InvalidDataException(String s) {
        super(s);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }
}
