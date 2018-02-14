package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:20
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class IncorrectOperationException extends RuntimeException {
    public IncorrectOperationException() {
        super();
    }

    public IncorrectOperationException( String message) {
        super(message);
    }

    public IncorrectOperationException(Throwable t) {
        super(t);
    }

    public IncorrectOperationException( String message, Throwable t) {
        super(message, t);
    }

    /** @deprecated use {@link #IncorrectOperationException(String, Throwable)} (to be removed in IDEA 15) */
    public IncorrectOperationException( String message, Exception e) {
        super(message, e);
    }
}
