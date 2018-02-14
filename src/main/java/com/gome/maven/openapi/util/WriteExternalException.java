package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 17:01
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class WriteExternalException extends Exception {
    public WriteExternalException() {
        super();
    }

    public WriteExternalException(String s) {
        super(s);
    }

    public WriteExternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteExternalException(Throwable cause) {
        super(cause);
    }
}
