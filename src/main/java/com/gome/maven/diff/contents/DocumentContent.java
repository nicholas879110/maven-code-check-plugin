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
package com.gome.maven.diff.contents;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

import java.nio.charset.Charset;

public interface DocumentContent extends DiffContent {
    /**
     * Represents this content as Document
     */
    
    Document getDocument();

    /**
     * This file could be used for better syntax highlighting.
     * Some file types can't be highlighted properly depending only on their FileType (ex: SQL dialects, PHP templates).
     */
    
    VirtualFile getHighlightFile();

    /**
     * Provides a way to open given text place in editor
     */
    
    OpenFileDescriptor getOpenFileDescriptor(int offset);

    /**
     * @return original file line separator
     */
    
    LineSeparator getLineSeparator();

    /**
     * @return original file charset
     */
    
    Charset getCharset();
}
