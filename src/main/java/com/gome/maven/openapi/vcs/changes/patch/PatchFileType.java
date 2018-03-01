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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 17.11.2006
 * Time: 17:36:42
 */
package com.gome.maven.openapi.vcs.changes.patch;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.structureView.StructureViewBuilder;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;

public class PatchFileType implements FileType {
    public static final String NAME = "PATCH";

    
    
    public String getName() {
        return NAME;
    }

    
    public String getDescription() {
        return VcsBundle.message("patch.file.type.description");
    }

    
    
    public String getDefaultExtension() {
        return "patch";
    }

    
    public Icon getIcon() {
        return AllIcons.Nodes.Pointcut;
    }

    public boolean isBinary() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    
    
    public String getCharset( VirtualFile file,  final byte[] content) {
        return null;
    }

    
    public SyntaxHighlighter getHighlighter( Project project, final VirtualFile virtualFile) {
        return null;
    }

    
    public StructureViewBuilder getStructureViewBuilder( VirtualFile file,  Project project) {
        return null;
    }
}
