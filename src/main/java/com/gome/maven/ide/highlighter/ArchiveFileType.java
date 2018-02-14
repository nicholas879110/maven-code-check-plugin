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
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;

public class ArchiveFileType implements FileType {
    private static final NotNullLazyValue<Icon> ICON = new NotNullLazyValue<Icon>() {
        
        @Override
        protected Icon compute() {
            return null;
//            return AllIcons.FileTypes.Archive;
        }
    };

    public static final ArchiveFileType INSTANCE = new ArchiveFileType();

    @Override
    
    public String getName() {
        return "ARCHIVE";
    }

    @Override
    
    public String getDescription() {
        return IdeBundle.message("filetype.description.archive.files");
    }

    @Override
    
    public String getDefaultExtension() {
        return "";
    }

    @Override
    public Icon getIcon() {
        return ICON.getValue();
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getCharset( VirtualFile file,  final byte[] content) {
        return null;
    }
}
