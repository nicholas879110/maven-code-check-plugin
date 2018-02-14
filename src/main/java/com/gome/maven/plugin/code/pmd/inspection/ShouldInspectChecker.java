package com.gome.maven.plugin.code.pmd.inspection;

import java.io.File;

/**
 * @author zhangliewei
 * @date 2017/12/20 16:44
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface ShouldInspectChecker {
    /**
     * check inspect whether or not
     *
     * @param file file to inspect
     * @return true or false
     */
    Boolean shouldInspect(File file);
}
