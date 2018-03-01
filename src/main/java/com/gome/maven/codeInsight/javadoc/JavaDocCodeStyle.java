package com.gome.maven.codeInsight.javadoc;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

public abstract class JavaDocCodeStyle {
    public static JavaDocCodeStyle getInstance(Project project) {
        return ServiceManager.getService(project, JavaDocCodeStyle.class);
    }

    public abstract boolean spaceBeforeComma();
    public abstract boolean spaceAfterComma();
}
