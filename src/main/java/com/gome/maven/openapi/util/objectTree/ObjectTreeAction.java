package com.gome.maven.openapi.util.objectTree;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:28
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface ObjectTreeAction<T> {

    void execute( T each);

    void beforeTreeExecution( T parent);

}
