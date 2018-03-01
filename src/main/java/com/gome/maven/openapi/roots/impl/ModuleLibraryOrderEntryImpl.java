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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.impl.libraries.LibraryEx;
import com.gome.maven.openapi.roots.impl.libraries.LibraryImpl;
import com.gome.maven.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.PersistentLibraryKind;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import com.gome.maven.util.PathUtil;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.java.JpsJavaModelSerializerExtension;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 * Library entry for module ("in-place") libraries
 *  @author dsl
 */
public class ModuleLibraryOrderEntryImpl extends LibraryOrderEntryBaseImpl implements LibraryOrderEntry, ClonableOrderEntry, WritableOrderEntry {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.roots.impl.LibraryOrderEntryImpl");
    private final Library myLibrary;
     public static final String ENTRY_TYPE = JpsModuleRootModelSerializer.MODULE_LIBRARY_TYPE;
    private boolean myExported;
     public static final String EXPORTED_ATTR = JpsJavaModelSerializerExtension.EXPORTED_ATTRIBUTE;

    //cloning
    private ModuleLibraryOrderEntryImpl(Library library, RootModelImpl rootModel, boolean isExported, DependencyScope scope) {
        super(rootModel, ProjectRootManagerImpl.getInstanceImpl(rootModel.getProject()));
        myLibrary = ((LibraryImpl)library).cloneLibrary(getRootModel());
        doinit();
        myExported = isExported;
        myScope = scope;
    }

    ModuleLibraryOrderEntryImpl(String name, final PersistentLibraryKind kind, RootModelImpl rootModel, ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        myLibrary = LibraryTableImplUtil.createModuleLevelLibrary(name, kind, getRootModel());
        doinit();
    }

    ModuleLibraryOrderEntryImpl(Element element, RootModelImpl rootModel, ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
        super(rootModel, projectRootManager);
        LOG.assertTrue(ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR)));
        myExported = element.getAttributeValue(EXPORTED_ATTR) != null;
        myScope = DependencyScope.readExternal(element);
        myLibrary = LibraryTableImplUtil.loadLibrary(element, getRootModel());
        doinit();
    }

    private void doinit() {
        Disposer.register(this, myLibrary);
        init();
    }

    @Override
    protected RootProvider getRootProvider() {
        return myLibrary.getRootProvider();
    }

    @Override
    public Library getLibrary() {
        return myLibrary;
    }

    @Override
    public boolean isModuleLevel() {
        return true;
    }

    @Override
    public String getLibraryName() {
        return myLibrary.getName();
    }

    @Override
    public String getLibraryLevel() {
        return LibraryTableImplUtil.MODULE_LEVEL;
    }

    
    @Override
    public String getPresentableName() {
        final String name = myLibrary.getName();
        if (name != null) {
            return name;
        }
        else {
            if (myLibrary instanceof LibraryEx && ((LibraryEx)myLibrary).isDisposed()) {
                return "<unknown>";
            }

            final String[] urls = myLibrary.getUrls(OrderRootType.CLASSES);
            if (urls.length > 0) {
                String url = urls[0];
                return PathUtil.toPresentableUrl(url);
            }
            else {
                return ProjectBundle.message("library.empty.library.item");
            }
        }
    }

    @Override
    public boolean isValid() {
        return !isDisposed() && myLibrary != null;
    }

    @Override
    public <R> R accept(RootPolicy<R> policy, R initialValue) {
        return policy.visitLibraryOrderEntry(this, initialValue);
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public OrderEntry cloneEntry(RootModelImpl rootModel,
                                 ProjectRootManagerImpl projectRootManager,
                                 VirtualFilePointerManager filePointerManager) {
        return new ModuleLibraryOrderEntryImpl(myLibrary, rootModel, myExported, myScope);
    }

    @Override
    public void writeExternal(Element rootElement) throws WriteExternalException {
        final Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
        if (myExported) {
            element.setAttribute(EXPORTED_ATTR, "");
        }
        myScope.writeExternal(element);
        myLibrary.writeExternal(element);
        rootElement.addContent(element);
    }


    @Override
    public boolean isExported() {
        return myExported;
    }

    @Override
    public void setExported(boolean value) {
        myExported = value;
    }

    @Override
    
    public DependencyScope getScope() {
        return myScope;
    }

    @Override
    public void setScope( DependencyScope scope) {
        myScope = scope;
    }
}
