//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class ScriptAvailability {
    public static boolean isJavaxScriptingAvailable() {
        Object engine = null;

        try {
            Class<?> managerClass = Class.forName("javax.script.ScriptEngineManager");
            Method m = managerClass.getMethod("getEngineByMimeType", String.class);
            engine = m.invoke(managerClass.newInstance(), "text/javascript");
        } catch (ClassNotFoundException var3) {
            ;
        } catch (NoSuchMethodException var4) {
            ;
        } catch (IllegalAccessException var5) {
            ;
        } catch (InvocationTargetException var6) {
            ;
        } catch (InstantiationException var7) {
            ;
        }

        return engine != null;
    }

    ScriptAvailability() {
    }
}
