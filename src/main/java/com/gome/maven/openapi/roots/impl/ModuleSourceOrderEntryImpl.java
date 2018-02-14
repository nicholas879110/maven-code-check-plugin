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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import com.gome.maven.util.ArrayUtil;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

import java.util.ArrayList;

/**
 *  @author dsl
 */
public class ModuleSourceOrderEntryImpl extends OrderEntryBaseImpl implements ModuleSourceOrderEntry, WritableOrderEntry, ClonableOrderEntry {
     static final String ENTRY_TYPE = JpsModuleRootModelSerializer.SOURCE_FOLDER_TYPE;
     private static final String ATTRIBUTE_FOR_TESTS = "forTests";

    ModuleSourceOrderEntryImpl(RootModelImpl rootModel) {
        super(rootModel);
    }

    ModuleSourceOrderEntryImpl(Element element, RootModelImpl rootModel) throws InvalidDataException {
        super(rootModel);
        if (!element.getName().equals(OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME)) {
            throw new InvalidDataException();
        }
    }

    @Override
    public void writeExternal(Element rootElement) throws WriteExternalException {
        Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
        element.setAttribute(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR, ENTRY_TYPE);
        element.setAttribute(ATTRIBUTE_FOR_TESTS, Boolean.FALSE.toString()); // compatibility with prev builds
        rootElement.addContent(element);
    }

    @Override
    public boolean isValid() {
        return !isDisposed();
    }

    @Override
    
    public Module getOwnerModule() {
        return getRootModel().getModule();
    }

    @Override
    public <R> R accept(RootPolicy<R> policy, R initialValue) {
        return policy.visitModuleSourceOrderEntry(this, initialValue);
    }

    @Override
    
    public String getPresentableName() {
        return ProjectBundle.message("project.root.module.source");
    }


    @Override
    
    public VirtualFile[] getFiles(OrderRootType type) {
        if (OrderRootType.SOURCES.equals(type)) {
            return getRootModel().getSourceRoots();
        }
        return VirtualFile.EMPTY_ARRAY;
    }

    @Override
    
    public String[] getUrls(OrderRootType type) {
        final ArrayList<String> result = new ArrayList<String>();
        if (OrderRootType.SOURCES.equals(type)) {
            for (ContentEntry contentEntry : getRootModel().getContentEntries()) {
                for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                    result.add(sourceFolder.getUrl());
                }
            }
            return ArrayUtil.toStringArray(result);
        }
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public OrderEntry cloneEntry(RootModelImpl rootModel,
                                 ProjectRootManagerImpl projectRootManager,
                                 VirtualFilePointerManager filePointerManager) {
        return new ModuleSourceOrderEntryImpl(rootModel);
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
