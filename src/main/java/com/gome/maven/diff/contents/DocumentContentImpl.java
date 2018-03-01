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
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

import java.nio.charset.Charset;

/**
 * Allows to compare some text associated with document.
 */
public class DocumentContentImpl implements DiffContent, DocumentContent {
     private final Document myDocument;

     private final FileType myType;
     private final VirtualFile myHighlightFile;

     private final LineSeparator mySeparator;
     private final Charset myCharset;

    public DocumentContentImpl( Document document) {
        this(document, null, null, null, null);
    }

    public DocumentContentImpl( Document document,
                                FileType type,
                                VirtualFile highlightFile,
                                LineSeparator separator,
                                Charset charset) {
        myDocument = document;
        myType = type;
        myHighlightFile = highlightFile;
        mySeparator = separator;
        myCharset = charset;
    }

    
    @Override
    public Document getDocument() {
        return myDocument;
    }

    
    @Override
    public VirtualFile getHighlightFile() {
        return myHighlightFile;
    }

    
    @Override
    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        return null;
    }

    
    @Override
    public OpenFileDescriptor getOpenFileDescriptor() {
        return getOpenFileDescriptor(0);
    }

    
    @Override
    public LineSeparator getLineSeparator() {
        return mySeparator;
    }

    
    @Override
    public FileType getContentType() {
        return myType;
    }

    
    @Override
    public Charset getCharset() {
        return myCharset;
    }

    @Override
    public void onAssigned(boolean isAssigned) {
    }
}
