package com.gome.maven.openapi.wm.ex;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public interface FrameEditorComponentProvider {
    ExtensionPointName<FrameEditorComponentProvider> EP = ExtensionPointName.create("com.gome.maven.frameEditorComponentProvider");

    JComponent createEditorComponent(Project project);
}
