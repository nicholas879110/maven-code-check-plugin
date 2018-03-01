/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.roots.ContentEntry;
import com.gome.maven.openapi.roots.ContentFolder;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointer;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 *  @author dsl
 */
public abstract class ContentFolderBaseImpl extends RootModelComponentBase implements ContentFolder, Comparable<ContentFolderBaseImpl> {
     public static final String URL_ATTRIBUTE = JpsModuleRootModelSerializer.URL_ATTRIBUTE;

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.roots.impl.SimpleContentFolderBaseImpl");

    private final VirtualFilePointer myFilePointer;
    protected final ContentEntryImpl myContentEntry;

    ContentFolderBaseImpl( VirtualFile file,  ContentEntryImpl contentEntry) {
        super(contentEntry.getRootModel());
        myContentEntry = contentEntry;
        myFilePointer = VirtualFilePointerManager.getInstance().create(file, this, null);
    }

    ContentFolderBaseImpl( String url,  ContentEntryImpl contentEntry) {
        super(contentEntry.getRootModel());
        myContentEntry = contentEntry;
        myFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);
    }

    protected ContentFolderBaseImpl( ContentFolderBaseImpl that,  ContentEntryImpl contentEntry) {
        this(that.myFilePointer, contentEntry);
    }

    ContentFolderBaseImpl( Element element,  ContentEntryImpl contentEntry) throws InvalidDataException {
        this(getUrlFrom(element), contentEntry);
    }

    protected ContentFolderBaseImpl( VirtualFilePointer filePointer,  ContentEntryImpl contentEntry) {
        super(contentEntry.getRootModel());
        myContentEntry = contentEntry;
        myFilePointer = VirtualFilePointerManager.getInstance().duplicate(filePointer,this, null);
    }

    private static String getUrlFrom(Element element) throws InvalidDataException {
        String url = element.getAttributeValue(URL_ATTRIBUTE);
        if (url == null) throw new InvalidDataException();
        return url;
    }

    @Override
    public VirtualFile getFile() {
        if (!myFilePointer.isValid()) {
            return null;
        }
        return myFilePointer.getFile();
    }

    @Override
    
    public ContentEntry getContentEntry() {
        return myContentEntry;
    }

    protected void writeFolder(Element element, String elementName) {
        LOG.assertTrue(element.getName().equals(elementName));
        element.setAttribute(URL_ATTRIBUTE, myFilePointer.getUrl());
    }

    @Override
    
    public String getUrl() {
        return myFilePointer.getUrl();
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public int compareTo(ContentFolderBaseImpl folder) {
        return getUrl().compareTo(folder.getUrl());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContentFolderBaseImpl)) return false;
        return compareTo((ContentFolderBaseImpl)obj) == 0;
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

    @Override
    public String toString() {
        return myFilePointer == null ? null : getUrl();
    }
}
