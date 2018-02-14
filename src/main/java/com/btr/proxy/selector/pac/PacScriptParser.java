//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

public interface PacScriptParser {
    PacScriptSource getScriptSource();

    String evaluate(String var1, String var2) throws ProxyEvaluationException;
}
