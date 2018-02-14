////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by Fernflower decompiler)
////
//
//package com.btr.proxy.selector.pac;
//
//import com.btr.proxy.util.Logger;
//import com.btr.proxy.util.Logger.LogLevel;
//import java.util.Calendar;
//import org.mozilla.javascript.Context;
//import org.mozilla.javascript.ContextFactory;
//import org.mozilla.javascript.Scriptable;
//import org.mozilla.javascript.ScriptableObject;
//
//public class RhinoPacScriptParser extends ScriptableObject implements PacScriptParser {
//    private static final long serialVersionUID = 1L;
//    private static final String[] JS_FUNCTION_NAMES = new String[]{"shExpMatch", "dnsResolve", "isResolvable", "isInNet", "dnsDomainIs", "isPlainHostName", "myIpAddress", "dnsDomainLevels", "localHostOrDomainIs", "weekdayRange", "dateRange", "timeRange"};
//    private Scriptable scope;
//    private PacScriptSource source;
//    private static final PacScriptMethods SCRIPT_METHODS = new PacScriptMethods();
//
//    public RhinoPacScriptParser(PacScriptSource source) throws ProxyEvaluationException {
//        this.source = source;
//        this.setupEngine();
//    }
//
//    public void setupEngine() throws ProxyEvaluationException {
//        Context context = (new ContextFactory()).enterContext();
//
//        try {
//            this.defineFunctionProperties(JS_FUNCTION_NAMES, RhinoPacScriptParser.class, 2);
//        } catch (Exception var3) {
//            Logger.log(this.getClass(), LogLevel.ERROR, "JS Engine setup error.", new Object[]{var3});
//            throw new ProxyEvaluationException(var3.getMessage(), var3);
//        }
//
//        this.scope = context.initStandardObjects(this);
//    }
//
//    public PacScriptSource getScriptSource() {
//        return this.source;
//    }
//
//    public String evaluate(String url, String host) throws ProxyEvaluationException {
//        try {
//            StringBuilder script = new StringBuilder(this.source.getScriptContent());
//            String evalMethod = " ;FindProxyForURL (\"" + url + "\",\"" + host + "\")";
//            script.append(evalMethod);
//            Context context = Context.enter();
//
//            String var7;
//            try {
//                Object result = context.evaluateString(this.scope, script.toString(), "userPacFile", 1, (Object)null);
//                var7 = Context.toString(result);
//            } finally {
//                Context.exit();
//            }
//
//            return var7;
//        } catch (Exception var12) {
//            Logger.log(this.getClass(), LogLevel.ERROR, "JS evaluation error.", new Object[]{var12});
//            throw new ProxyEvaluationException("Error while executing PAC script: " + var12.getMessage(), var12);
//        }
//    }
//
//    public String getClassName() {
//        return this.getClass().getSimpleName();
//    }
//
//    public static boolean isPlainHostName(String host) {
//        return SCRIPT_METHODS.isPlainHostName(host);
//    }
//
//    public static boolean dnsDomainIs(String host, String domain) {
//        return SCRIPT_METHODS.dnsDomainIs(host, domain);
//    }
//
//    public static boolean localHostOrDomainIs(String host, String domain) {
//        return SCRIPT_METHODS.localHostOrDomainIs(host, domain);
//    }
//
//    public static boolean isResolvable(String host) {
//        return SCRIPT_METHODS.isResolvable(host);
//    }
//
//    public static boolean isInNet(String host, String pattern, String mask) {
//        return SCRIPT_METHODS.isInNet(host, pattern, mask);
//    }
//
//    public static String dnsResolve(String host) {
//        return SCRIPT_METHODS.dnsResolve(host);
//    }
//
//    public static String myIpAddress() {
//        return SCRIPT_METHODS.myIpAddress();
//    }
//
//    public static int dnsDomainLevels(String host) {
//        return SCRIPT_METHODS.dnsDomainLevels(host);
//    }
//
//    public static boolean shExpMatch(String str, String shexp) {
//        return SCRIPT_METHODS.shExpMatch(str, shexp);
//    }
//
//    public static boolean weekdayRange(String wd1, String wd2, String gmt) {
//        return SCRIPT_METHODS.weekdayRange(wd1, wd2, gmt);
//    }
//
//    static void setCurrentTime(Calendar cal) {
//        SCRIPT_METHODS.setCurrentTime(cal);
//    }
//
//    public static boolean dateRange(Object day1, Object month1, Object year1, Object day2, Object month2, Object year2, Object gmt) {
//        return SCRIPT_METHODS.dateRange(day1, month1, year1, day2, month2, year2, gmt);
//    }
//
//    public static boolean timeRange(Object hour1, Object min1, Object sec1, Object hour2, Object min2, Object sec2, Object gmt) {
//        return SCRIPT_METHODS.timeRange(hour1, min1, sec1, hour2, min2, sec2, gmt);
//    }
//}
