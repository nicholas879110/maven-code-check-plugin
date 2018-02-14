/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.ide.highlighter;

//import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.fileTypes.InternalFileType;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;


import javax.swing.*;

public class WorkspaceFileType implements InternalFileType {
    public static final WorkspaceFileType INSTANCE = new WorkspaceFileType();

     public static final String DEFAULT_EXTENSION = "iws";
     public static final String DOT_DEFAULT_EXTENSION = "." + DEFAULT_EXTENSION;

    private WorkspaceFileType() {}

    @Override
    
    public String getName() {
        return "IDEA_WORKSPACE";
    }

    @Override
    
    public String getDescription() {
        return IdeBundle.message("filetype.description.idea.workspace");
    }

    @Override
    
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    public Icon getIcon() {
        return null;
       // return AllIcons.Nodes.IdeaWorkspace;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getCharset( VirtualFile file,  final byte[] content) {
        return CharsetToolkit.UTF8;
    }
}
