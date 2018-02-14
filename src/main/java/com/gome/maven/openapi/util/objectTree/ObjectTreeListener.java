package com.gome.maven.openapi.util.objectTree;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:29
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface ObjectTreeListener {

    void objectRegistered( Object node);
    void objectExecuted( Object node);

}
