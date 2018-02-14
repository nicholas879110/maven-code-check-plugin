package com.gome.maven.plugin.code.pmd.inspection;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.PrintStream;

public class ProblemDescriptor implements RuleViolation {

    //    private File file;
    private RuleViolation ruleViolation;

    public ProblemDescriptor(RuleViolation ruleViolation) {
//        this.file = file;
        this.ruleViolation = ruleViolation;
    }

    @Override
    public Rule getRule() {
        return ruleViolation.getRule();
    }

    @Override
    public String getDescription() {
        return ruleViolation.getDescription();
    }

    @Override
    public boolean isSuppressed() {
        return ruleViolation.isSuppressed();
    }

    @Override
    public String getFilename() {
        return  ruleViolation.getFilename();
    }

    @Override
    public int getBeginLine() {
        return ruleViolation.getBeginLine();
    }

    @Override
    public int getBeginColumn() {
        return ruleViolation.getBeginColumn();
    }

    @Override
    public int getEndLine() {
        return ruleViolation.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return ruleViolation.getEndColumn();
    }

    @Override
    public String getPackageName() {
        return ruleViolation.getPackageName();
    }

    @Override
    public String getClassName() {
        return ruleViolation.getClassName();
    }

    @Override
    public String getMethodName() {
        return ruleViolation.getMethodName();
    }

    @Override
    public String getVariableName() {
        return ruleViolation.getVariableName();
    }

    @Override
    public String toString() {
        if (ruleViolation != null) {
            if (ruleViolation.getFilename() != null) {
                String fileName=convertFilename(getFilename());
                String className=StringUtils.substringBeforeLast(fileName,".");
                if (getBeginLine() != 0) {
                    if (getBeginColumn() != 0) {
                        return "Alibaba-code-check:"+getRule().getName()+" at "+getPackageName()+"."+className+".$("+convertFilename(getFilename())+":"+getBeginLine()+")"+"\n"
                                +"Question Description:"+getDescription()+"\n"
                                +"Code examples :" + getRule().getMessage()+"\n"+getRule().getExamples();
//                        return "Alibaba code check : " + getRule().getName() + " at "  +  convertFilename(getFilename()) + " : " + "[" + getBeginLine() + "," + getBeginColumn() + "] \n"
//                                + "Description : " + getRule().getDescription()
//                                + "Code examples :" + getRule().getExamples();
                    } else {
//                        return "Alibaba code check : " + getRule().getName() + " at " +  convertFilename(getFilename())+ ":" + "[" + getBeginLine() + "] \n"
//                                + "Description : " + getRule().getDescription()
//                                + "Code examples :" + getRule().getExamples();
                        return "Alibaba-code-check:"+getRule().getName()+" at "+getPackageName()+"."+className+".$("+convertFilename(getFilename())+":"+getBeginLine()+")"+"\n"
                                +"Question Description:"+getDescription()+"\n"
                                +"Code examples :" + getRule().getMessage()+"\n"+getRule().getExamples();
                    }
                } else {
//                    return "Alibaba code check : " + getRule().getName() + " at " +  convertFilename(getFilename()) + ": \n"
//                            + "Description : " + getRule().getDescription()
//                            + "Code examples :" + getRule().getExamples();
                    return "Alibaba-code-check:"+getRule().getName()+" at "+getPackageName()+"."+className+".$("+convertFilename(getFilename())+":"+getBeginLine()+")"+"\n"
                            +"Question Description:"+getDescription()+"\n"
                            +"Code examples :" + getRule().getMessage()+"\n"+getRule().getExamples();
                }
            } else {
                return "ruleViolation  finename is null ";
            }

        } else {
            return "ruleViolation is null";
        }
    }

    public boolean isNativeMethod() {
        return getBeginLine() == -2;
    }

    public String msgString() {
        return getPackageName()+"."+getClassName() + (getMethodName()!=null?("." + getMethodName()):"" )+
                (isNativeMethod() ? "(Native Method)" :
                        (convertFilename(getFilename()) != null && getBeginLine() >= 0 ?
                                "(" + getFilename() + ":" + getBeginLine() + ")" :
                                (convertFilename(getFilename()) != null ?  "("+convertFilename(getFilename())+")" : "(Unknown Source)")));
    }


    public static void printStackTrace(PrintStream s) {
        StackTraceElement traceElement=new StackTraceElement("Aliba.rule","","PmdReport.java",308);
        s.println("\tat " + traceElement);
    }

    private static String convertFilename(String filename){
        if (StringUtils.isBlank(filename)){
            return "";
        }
        String tmp=StringUtils.substringAfterLast(filename,File.separator);
        if (tmp.length()==filename.length()){
            tmp=StringUtils.substringAfterLast(filename,"\\");
        }
        return tmp;
    }

    public static void main(String[] args) {
        String s="/E:/gome-project/maven-code-check-plugin/src/main/java/com/gome/maven/plugin/code/pmd/util/ReflectionUtil.java:[5,19]";
        System.err.println(s);
//        printStackTrace(System.out);
        System.out.println("Alibaba code che.ck:e at(PmdReport.java:308)");
        System.out.println("Alibaba code che.ck:e at(PmdReport.java)");
//        StackTraceElement traceElement=new StackTraceElement("com.gome.maven.plugin.code.check.PmdReport","getLocationTemp","PmdReport.java",308);
//
//        SystemStreamLog LOG = new SystemStreamLog();
//        LOG.warn(new Throwable());
//        LOG.warn(s);
//        CompilerMessage compilerMessage=new CompilerMessage("/E:/gome-project/maven-code-check-plugin/src/main/java/com/gome/maven/plugin/code/pmd/util/ReflectionUtil.java", CompilerMessage.Kind.WARNING,255,10,265,10,"ceshi");
//
//        ConsoleLogger consoleLogger= new ConsoleLogger(1,"debug");
//        consoleLogger.warn(compilerMessage.toString());
//
//        System.out.println("<html><head></head><body><a HREF=\"file://E:/gome-project/maven-code-check-plugin/src/main/java/com/gome/maven/plugin/code/pmd/inspection/CacheUtils.java#3397\">99</body></html>");
    }
}
