//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.psi.codeStyle;

/** @deprecated */
public interface Indent {
    boolean isGreaterThan(Indent var1);

    Indent min(Indent var1);

    Indent max(Indent var1);

    Indent add(Indent var1);

    Indent subtract(Indent var1);

    boolean isZero();
}
