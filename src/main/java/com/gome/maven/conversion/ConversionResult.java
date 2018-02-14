//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.conversion;

import com.gome.maven.openapi.project.Project;

public interface ConversionResult {
    boolean conversionNotNeeded();

    boolean openingIsCanceled();

    void postStartupActivity( Project var1);
}
