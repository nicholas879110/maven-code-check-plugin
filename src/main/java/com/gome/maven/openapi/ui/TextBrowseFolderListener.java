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
package com.gome.maven.openapi.ui;

import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

public class TextBrowseFolderListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {
    public TextBrowseFolderListener( FileChooserDescriptor fileChooserDescriptor) {
        this(fileChooserDescriptor, null);
    }

    public TextBrowseFolderListener( FileChooserDescriptor fileChooserDescriptor,  Project project) {
        super(null, null, null, project, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    }

    void setOwnerComponent( TextFieldWithBrowseButton component) {
        myTextComponent = component;
    }

    FileChooserDescriptor getFileChooserDescriptor() {
        return myFileChooserDescriptor;
    }
}