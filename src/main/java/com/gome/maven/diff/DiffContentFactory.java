/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff;

import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.contents.DocumentContent;
import com.gome.maven.diff.contents.EmptyContent;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.io.IOException;

/*
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
public abstract class DiffContentFactory {
    
    public static DiffContentFactory getInstance() {
        return ServiceManager.getService(DiffContentFactory.class);
    }

    
    public abstract EmptyContent createEmpty();

    
    public abstract DocumentContent create( String text);

    
    public abstract DocumentContent create( String text,  FileType type);

    
    public abstract DocumentContent create( String text,  FileType type, boolean respectLineSeparators);

    
    public abstract DocumentContent create( Project project,  Document document);

    
    public abstract DocumentContent create( Project project,  Document document,  VirtualFile file);

    
    public abstract DiffContent create( Project project,  VirtualFile file);

    
    public abstract DocumentContent createDocument( Project project,  VirtualFile file);

    
    public abstract DiffContent createClipboardContent();

    /**
     * @param referenceContent used to detect FileType and proper highlighting for clipboard content
     */
    
    public abstract DocumentContent createClipboardContent( DocumentContent referenceContent);

    
    public abstract DiffContent createBinary( Project project,
                                              String name,
                                              FileType type,
                                              byte[] content) throws IOException;
}
