package com.gome.maven.log;

import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author zhangliewei
 * @date 2017/12/20 15:15
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class CodeCheckSystemStreamLog extends SystemStreamLog{


    public void major(CharSequence content) {
        print("MAJOR", content);
    }


    public void major(CharSequence content, Throwable error) {
        print("MAJOR", content, error);
    }


    public void major(Throwable error) {
        print("MAJOR", error);
    }

    public void critical(CharSequence content) {
        print("CRITICAL", content);
    }


    public void critical(CharSequence content, Throwable error) {
        print("CRITICAL", content, error);
    }


    public void critical(Throwable error) {
        print("CRITICAL", error);
    }


    public void blocker(CharSequence content) {
        print("BLOCKER", content);
    }


    public void blocker(CharSequence content, Throwable error) {
        print("BLOCKER", content, error);
    }


    public void blocker(Throwable error) {
        print("BLOCKER", error);
    }


    private void print(String prefix, CharSequence content) {
        System.out.println("[" + prefix + "] " + content.toString());
    }

    private void print(String prefix, Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.out.println("[" + prefix + "] " + sWriter.toString());
    }

    private void print(String prefix, CharSequence content, Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.out.println("[" + prefix + "] " + content.toString() + "\n\n" + sWriter.toString());
    }

}
