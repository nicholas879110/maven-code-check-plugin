package com.gome.maven.plugin.code;

import java.io.PrintStream;

public class StackTest {

    public static void test(){
        try {
            int x=5/0;
            System.out.println(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        test();
        printStackTrace(System.err);
    }

    public static void printStackTrace(PrintStream s) {
        StackTraceElement traceElement=new StackTraceElement("com.gome.maven.plugin.code.check.PmdFileInfo","getSourceDirectory","PmdFileInfo.java",61);
        s.println("\tat " + traceElement);
    }
}
