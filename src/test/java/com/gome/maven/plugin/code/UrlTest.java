package com.gome.maven.plugin.code;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author zhangliewei
 * @date 2018/1/15 13:43
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class UrlTest {
    public static void main(String[] args) {
        try {
            URL url = new URL("file", "", -1, "/E:/gome-project/maven-code-check-plugin/resources/IdeaPlugin.xml");
            URL url1 = new URL(url,"IdeTipsAndTricks.xml");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
