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
package com.gome.maven.openapi.fileChooser;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public abstract class FileChooserFactory {
    public static FileChooserFactory getInstance() {
        return ServiceManager.getService(FileChooserFactory.class);
    }

    
    public abstract FileChooserDialog createFileChooser( FileChooserDescriptor descriptor,
                                                         Project project,
                                                         Component parent);

    
    public abstract PathChooserDialog createPathChooser( FileChooserDescriptor descriptor,
                                                         Project project,
                                                         Component parent);

    /**
     * Creates Save File dialog.
     *
     * @param descriptor dialog descriptor
     * @param project    chooser options
     * @return Save File dialog
     * @since 9.0
     */
    
    public abstract FileSaverDialog createSaveFileDialog( FileSaverDescriptor descriptor,  Project project);

    
    public abstract FileSaverDialog createSaveFileDialog( FileSaverDescriptor descriptor,  Component parent);

    
    public abstract FileTextField createFileTextField( FileChooserDescriptor descriptor, boolean showHidden,  Disposable parent);

    
    public FileTextField createFileTextField( FileChooserDescriptor descriptor,  Disposable parent) {
        return createFileTextField(descriptor, true, parent);
    }

    /**
     * Adds path completion listener to a given text field.
     *
     * @param field      input field to add completion to
     * @param descriptor chooser options
     * @param showHidden include hidden files into completion variants
     * @param parent     if null then will be registered with {@link com.gome.maven.openapi.actionSystem.PlatformDataKeys#UI_DISPOSABLE}
     */
    public abstract void installFileCompletion( JTextField field,
                                                FileChooserDescriptor descriptor,
                                               boolean showHidden,
                                                Disposable parent);
}
