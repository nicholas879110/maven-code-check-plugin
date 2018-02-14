/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.roots.ui.configuration;

import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.roots.SourceFolder;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public abstract class ModuleSourceRootEditHandler<P extends JpsElement> {
    public static final ExtensionPointName<ModuleSourceRootEditHandler> EP_NAME = ExtensionPointName.create("com.intellij.projectStructure.sourceRootEditHandler");
    private final JpsModuleSourceRootType<P> myRootType;

    protected ModuleSourceRootEditHandler(JpsModuleSourceRootType<P> rootType) {
        myRootType = rootType;
    }

    
    public static <P extends JpsElement> ModuleSourceRootEditHandler<P> getEditHandler( JpsModuleSourceRootType<P> type) {
        for (ModuleSourceRootEditHandler editor : EP_NAME.getExtensions()) {
            if (editor.getRootType().equals(type)) {
                return editor;
            }
        }
        throw new IllegalArgumentException("Cannot find edit handler for " + type);
    }

    public final JpsModuleSourceRootType<P> getRootType() {
        return myRootType;
    }

    
    public abstract String getRootTypeName();

    
    public abstract Icon getRootIcon();

    
    public Icon getRootIcon( P properties) {
        return getRootIcon();
    }

    
    public abstract Icon getFolderUnderRootIcon();

    
    public abstract CustomShortcutSet getMarkRootShortcutSet();

    
    public abstract String getRootsGroupTitle();

    
    public abstract Color getRootsGroupColor();


    
    public String getMarkRootButtonText() {
        return getRootTypeName();
    }

    
    public abstract String getUnmarkRootButtonText();

    
    public String getPropertiesString( P properties) {
        return null;
    }

    
    public JComponent createPropertiesEditor( SourceFolder folder,  JComponent parentComponent,
                                              ContentRootPanel.ActionCallback callback) {
        return null;
    }
}
