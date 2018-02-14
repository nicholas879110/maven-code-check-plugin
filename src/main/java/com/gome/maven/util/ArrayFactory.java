package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2017/12/29 16:04
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface ArrayFactory<T> {
    T[] create(int count);
}
