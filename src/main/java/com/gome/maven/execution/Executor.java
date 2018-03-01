//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.execution;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.text.StringUtil;
import javax.swing.Icon;

public abstract class Executor {
    public static final ExtensionPointName<Executor> EXECUTOR_EXTENSION_NAME = ExtensionPointName.create("com.gome.maven.executor");

    public Executor() {
    }

    public abstract String getToolWindowId();

    public abstract Icon getToolWindowIcon();

    
    public abstract Icon getIcon();

    public abstract Icon getDisabledIcon();

    public abstract String getDescription();

    
    public abstract String getActionName();

    
    
    public abstract String getId();

    
    public abstract String getStartActionText();

    
    public abstract String getContextActionId();

    
    public abstract String getHelpId();

    public String getStartActionText(String configurationName) {
        return this.getStartActionText() + (StringUtil.isEmpty(configurationName) ? "" : " '" + StringUtil.first(configurationName, 30, true) + "'");
    }
}
