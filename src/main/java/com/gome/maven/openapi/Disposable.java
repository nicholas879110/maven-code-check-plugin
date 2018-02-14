package com.gome.maven.openapi;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:05
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Disposable {
    void dispose();

    interface Parent extends Disposable {
        void beforeTreeDispose();
    }

}
