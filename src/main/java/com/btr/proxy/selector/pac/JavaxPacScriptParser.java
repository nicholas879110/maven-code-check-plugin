//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.selector.pac;

import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;
import java.lang.reflect.Method;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavaxPacScriptParser implements PacScriptParser {
    static final String SCRIPT_METHODS_OBJECT = "__pacutil";
    private final PacScriptSource source;
    private final ScriptEngine engine;

    public JavaxPacScriptParser(PacScriptSource source) throws ProxyEvaluationException {
        this.source = source;
        this.engine = this.setupEngine();
    }

    private ScriptEngine setupEngine() throws ProxyEvaluationException {
        ScriptEngineManager mng = new ScriptEngineManager();
        ScriptEngine engine = mng.getEngineByMimeType("text/javascript");
        engine.put("__pacutil", new PacScriptMethods());
        Class<?> scriptMethodsClazz = ScriptMethods.class;
        Method[] scriptMethods = scriptMethodsClazz.getMethods();
        Method[] arr$ = scriptMethods;
        int len$ = scriptMethods.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            String name = method.getName();
            int args = method.getParameterTypes().length;
            StringBuilder toEval = (new StringBuilder(name)).append(" = function(");

            for(int i = 0; i < args; ++i) {
                if (i > 0) {
                    toEval.append(",");
                }

                toEval.append("arg").append(i);
            }

            toEval.append(") {return ");
            String functionCall = this.buildFunctionCallCode(name, args);
            if (String.class.isAssignableFrom(method.getReturnType())) {
                functionCall = "String(" + functionCall + ")";
            }

            toEval.append(functionCall).append("; }");

            try {
                engine.eval(toEval.toString());
            } catch (ScriptException var14) {
                Logger.log(this.getClass(), LogLevel.ERROR, "JS evaluation error when creating alias for " + name + ".", new Object[]{var14});
                throw new ProxyEvaluationException("Error setting up script engine", var14);
            }
        }

        return engine;
    }

    private String buildFunctionCallCode(String functionName, int args) {
        StringBuilder functionCall = new StringBuilder();
        functionCall.append("__pacutil").append(".").append(functionName).append("(");

        for(int i = 0; i < args; ++i) {
            if (i > 0) {
                functionCall.append(",");
            }

            functionCall.append("arg").append(i);
        }

        functionCall.append(")");
        return functionCall.toString();
    }

    public PacScriptSource getScriptSource() {
        return this.source;
    }

    public String evaluate(String url, String host) throws ProxyEvaluationException {
        try {
            StringBuilder script = new StringBuilder(this.source.getScriptContent());
            String evalMethod = " ;FindProxyForURL (\"" + url + "\",\"" + host + "\")";
            script.append(evalMethod);
            Object result = this.engine.eval(script.toString());
            return (String)result;
        } catch (Exception var6) {
            Logger.log(this.getClass(), LogLevel.ERROR, "JS evaluation error.", new Object[]{var6});
            throw new ProxyEvaluationException("Error while executing PAC script: " + var6.getMessage(), var6);
        }
    }
}
