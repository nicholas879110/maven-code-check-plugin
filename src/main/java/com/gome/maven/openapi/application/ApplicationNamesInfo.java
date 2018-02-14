//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.application;

import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import org.jdom.Element;

public class ApplicationNamesInfo {
    
    private static final String COMPONENT_NAME = "ApplicationInfo";
    
    private static final String ELEMENT_NAMES = "names";
    
    private static final String ATTRIBUTE_PRODUCT = "product";
    
    private static final String ATTRIBUTE_FULL_NAME = "fullname";
    
    private static final String ATTRIBUTE_SCRIPT = "script";
    private String myProductName;
    private String myFullProductName;
    private String myLowercaseProductName;
    private String myScriptName;

    
    public static ApplicationNamesInfo getInstance() {
        return ApplicationNamesInfo.ApplicationNamesInfoHolder.ourInstance;
    }

    private ApplicationNamesInfo() {
        try {
            this.readInfo(JDOMUtil.load(ApplicationNamesInfo.class.getResourceAsStream("/idea/" + getComponentName() + ".xml")));
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    private void readInfo(Element rootElement) {
        Element names = rootElement.getChild("names");
        this.myProductName = names.getAttributeValue("product");
        this.myFullProductName = names.getAttributeValue("fullname");
        this.myLowercaseProductName = StringUtil.capitalize(this.myProductName.toLowerCase());
        this.myScriptName = names.getAttributeValue("script");
    }

    public String getProductName() {
        return this.myProductName;
    }

    public String getFullProductName() {
        return this.myFullProductName;
    }

    public String getLowercaseProductName() {
        return this.myLowercaseProductName;
    }

    public String getScriptName() {
        return this.myScriptName;
    }

    public static String getComponentName() {
        String prefix = System.getProperty("idea.platform.prefix");
        return prefix != null ? prefix + "ApplicationInfo" : "ApplicationInfo";
    }

    private static class ApplicationNamesInfoHolder {
        private static final ApplicationNamesInfo ourInstance = new ApplicationNamesInfo();

        private ApplicationNamesInfoHolder() {
        }
    }
}
