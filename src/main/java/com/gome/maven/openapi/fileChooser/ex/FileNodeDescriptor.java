/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.fileChooser.ex;

import com.gome.maven.ide.util.treeView.NodeDescriptor;
import com.gome.maven.openapi.fileChooser.FileElement;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.IconLoader;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.SimpleTextAttributes;

import javax.swing.*;

public class FileNodeDescriptor extends NodeDescriptor {

    private FileElement myFileElement;
    private final Icon myOriginalIcon;
    private final String myComment;

    public FileNodeDescriptor(Project project,
                               FileElement element,
                              NodeDescriptor parentDescriptor,
                              Icon closedIcon,
                              String name,
                              String comment) {
        super(project, parentDescriptor);
        myOriginalIcon = closedIcon;
        myComment = comment;
        myFileElement = element;
        myName = name;
    }

    public boolean update() {
        boolean changed = false;

        // special handling for roots with names (e.g. web roots)
        if (myName == null || myComment == null) {
            final String newName = myFileElement.toString();
            if (!newName.equals(myName)) changed = true;
            myName = newName;
        }

        VirtualFile file = myFileElement.getFile();

        if (file == null) return true;

        setIcon(myOriginalIcon);
        if (myFileElement.isHidden()) {
            setIcon(IconLoader.getTransparentIcon(getIcon()));
        }
        myColor = myFileElement.isHidden() ? SimpleTextAttributes.DARK_TEXT.getFgColor() : null;
        return changed;
    }

    
    public final FileElement getElement() {
        return myFileElement;
    }

    protected final void setElement(FileElement descriptor) {
        myFileElement = descriptor;
    }

    public String getComment() {
        return myComment;
    }
}
