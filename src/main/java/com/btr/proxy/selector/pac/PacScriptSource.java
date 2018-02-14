//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import java.io.IOException;

public interface PacScriptSource {
    String getScriptContent() throws IOException;

    boolean isScriptValid();
}
