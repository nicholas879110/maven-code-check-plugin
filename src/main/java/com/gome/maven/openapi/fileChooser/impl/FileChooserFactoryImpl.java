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
package com.gome.maven.openapi.fileChooser.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathMacros;
import com.gome.maven.openapi.fileChooser.*;
import com.gome.maven.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.gome.maven.openapi.fileChooser.ex.FileSaverDialogImpl;
import com.gome.maven.openapi.fileChooser.ex.FileTextFieldImpl;
import com.gome.maven.openapi.fileChooser.ex.LocalFsFinder;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.registry.Registry;
//import com.gome.maven.ui.mac.MacFileChooserDialogImpl;
import com.gome.maven.util.SystemProperties;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileChooserFactoryImpl extends FileChooserFactory {
    
    @Override
    public FileChooserDialog createFileChooser( FileChooserDescriptor descriptor,
                                                Project project,
                                                Component parent) {
        if (parent != null) {
            return new FileChooserDialogImpl(descriptor, parent, project);
        }
        else {
            return new FileChooserDialogImpl(descriptor, project);
        }
    }

    
    @Override
    public PathChooserDialog createPathChooser( FileChooserDescriptor descriptor,
                                                Project project,
                                                Component parent) {
        if (useNativeMacChooser(descriptor)) {
            return null;//自己修改的new MacFileChooserDialogImpl(descriptor, project);
        }
        else if (parent != null) {
            return new FileChooserDialogImpl(descriptor, parent, project);
        }
        else {
            return new FileChooserDialogImpl(descriptor, project);
        }
    }

    private static boolean useNativeMacChooser(final FileChooserDescriptor descriptor) {
        return SystemInfo.isMac &&
                !descriptor.isChooseJarContents() &&
                SystemProperties.getBooleanProperty("native.mac.file.chooser.enabled", true) &&
                Registry.is("ide.mac.file.chooser.native") &&
                !DialogWrapper.isMultipleModalDialogs();
    }

    
    @Override
    public FileTextField createFileTextField( final FileChooserDescriptor descriptor, boolean showHidden,  Disposable parent) {
        return new FileTextFieldImpl.Vfs(new JTextField(), getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
    }

    @Override
    public void installFileCompletion( JTextField field,
                                       FileChooserDescriptor descriptor,
                                      boolean showHidden,
                                       Disposable parent) {
        if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
            new FileTextFieldImpl.Vfs(field, getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
        }
    }

    public static Map<String, String> getMacroMap() {
        final PathMacros macros = PathMacros.getInstance();
        final Set<String> allNames = macros.getAllMacroNames();
        final HashMap<String, String> map = new HashMap<String, String>();
        for (String eachMacroName : allNames) {
            map.put("$" + eachMacroName + "$", macros.getValue(eachMacroName));
        }

        return map;
    }

    
    @Override
    public FileSaverDialog createSaveFileDialog( FileSaverDescriptor descriptor,  Project project) {
        return new FileSaverDialogImpl(descriptor, project);
    }

    
    @Override
    public FileSaverDialog createSaveFileDialog( FileSaverDescriptor descriptor,  Component parent) {
        return new FileSaverDialogImpl(descriptor, parent);
    }
}
