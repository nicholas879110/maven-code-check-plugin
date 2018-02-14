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
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.impl.libraries.LibraryEx;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.LibraryTable;
import com.gome.maven.openapi.roots.libraries.LibraryTablesRegistrar;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;
import org.jetbrains.jps.model.serialization.java.JpsJavaModelSerializerExtension;

/**
 *  @author dsl
 */
class LibraryOrderEntryImpl extends LibraryOrderEntryBaseImpl implements LibraryOrderEntry, ClonableOrderEntry, WritableOrderEntry {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.LibraryOrderEntryImpl");
    private Library myLibrary;
     private String myLibraryName; // is non-null if myLibrary == null
     private String myLibraryLevel; // is non-null if myLibraryLevel == null
    private boolean myExported;
     static final String ENTRY_TYPE = JpsModuleRootModelSerializer.LIBRARY_TYPE;
     private static final String NAME_ATTR = JpsModuleRootModelSerializer.NAME_ATTRIBUTE;
     private static final String LEVEL_ATTR = JpsModuleRootModelSerializer.LEVEL_ATTRIBUTE;
    private final MyOrderEntryLibraryTableListener myLibraryListener = new MyOrderEntryLibraryTableListener();
     private static final String EXPORTED_ATTR = JpsJavaModelSerializerExtension.EXPORTED_ATTRIBUTE;

    LibraryOrderEntryImpl( Library library,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        LOG.assertTrue(library.getTable() != null);
        myLibrary = library;
        addListeners();
        init();
    }

    LibraryOrderEntryImpl( Element element,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
        super(rootModel, projectRootManager);
        LOG.assertTrue(ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR)));
        myExported = element.getAttributeValue(EXPORTED_ATTR) != null;
        myScope = DependencyScope.readExternal(element);
        String level = element.getAttributeValue(LEVEL_ATTR);
        String name = element.getAttributeValue(NAME_ATTR);
        if (name == null) throw new InvalidDataException();
        if (level == null) throw new InvalidDataException();
        searchForLibrary(name, level);
        addListeners();
        init();
    }

    private LibraryOrderEntryImpl( LibraryOrderEntryImpl that,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) {
        super (rootModel, projectRootManager);
        if (that.myLibrary == null) {
            myLibraryName = that.myLibraryName;
            myLibraryLevel = that.myLibraryLevel;
        }
        else {
            myLibrary = that.myLibrary;
        }
        myExported = that.myExported;
        myScope = that.myScope;
        addListeners();
        init();
    }

    public LibraryOrderEntryImpl( String name,
                                  String level,
                                  RootModelImpl rootModel,
                                  ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        searchForLibrary(name, level);
        addListeners();
    }

    private void searchForLibrary( String name,  String level) {
        if (myLibrary != null) return;
        final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, getRootModel().getModule().getProject());
        final Library library = libraryTable != null ? libraryTable.getLibraryByName(name) : null;
        if (library == null) {
            myLibraryName = name;
            myLibraryLevel = level;
            myLibrary = null;
        }
        else {
            myLibraryName = null;
            myLibraryLevel = null;
            myLibrary = library;
        }
    }

    @Override
    public boolean isExported() {
        return myExported;
    }

    @Override
    public void setExported(boolean exported) {
        myExported = exported;
    }

    @Override
    
    public DependencyScope getScope() {
        return myScope;
    }

    @Override
    public void setScope( DependencyScope scope) {
        myScope = scope;
    }

    @Override
    
    public Library getLibrary() {
        Library library = getRootModel().getConfigurationAccessor().getLibrary(myLibrary, myLibraryName, myLibraryLevel);
        if (library != null) { //library was not deleted
            return library;
        }
        if (myLibrary != null) {
            myLibraryName = myLibrary.getName();
            myLibraryLevel = myLibrary.getTable().getTableLevel();
        }
        myLibrary = null;
        return null;
    }

    @Override
    public boolean isModuleLevel() {
        return false;
    }

    
    @Override
    public String getPresentableName() {
        return getLibraryName();
    }

    @Override
    
    protected RootProvider getRootProvider() {
        return myLibrary == null ? null : myLibrary.getRootProvider();
    }

    @Override
    public boolean isValid() {
        if (isDisposed()) {
            return false;
        }
        Library library = getLibrary();
        return library != null && !((LibraryEx)library).isDisposed();
    }

    @Override
    public <R> R accept( RootPolicy<R> policy, R initialValue) {
        return policy.visitLibraryOrderEntry(this, initialValue);
    }

    @Override
    
    public OrderEntry cloneEntry( RootModelImpl rootModel,
                                 ProjectRootManagerImpl projectRootManager,
                                 VirtualFilePointerManager filePointerManager) {
        ProjectRootManagerImpl rootManager = ProjectRootManagerImpl.getInstanceImpl(getRootModel().getModule().getProject());
        return new LibraryOrderEntryImpl(this, rootModel, rootManager);
    }

    @Override
    public void writeExternal( Element rootElement) throws WriteExternalException {
        final Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
        final String libraryLevel = getLibraryLevel();
        if (myExported) {
            element.setAttribute(EXPORTED_ATTR, "");
        }
        myScope.writeExternal(element);
        element.setAttribute(NAME_ATTR, getLibraryName());
        element.setAttribute(LEVEL_ATTR, libraryLevel);
        rootElement.addContent(element);
    }

    @Override
    
    public String getLibraryLevel() {
        if (myLibrary != null) {
            final LibraryTable table = myLibrary.getTable();
            return table.getTableLevel();
        } else {
            return myLibraryLevel;
        }
    }

    @Override
    public String getLibraryName() {
        return myLibrary == null ? myLibraryName : myLibrary.getName();
    }

    private void addListeners () {
        final String libraryLevel = getLibraryLevel();
        final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(libraryLevel, getRootModel().getModule().getProject());
        if (libraryTable != null) {
            myProjectRootManagerImpl.addListenerForTable(myLibraryListener, libraryTable);
        }
    }


    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public void dispose() {
        super.dispose();
        final LibraryTable libraryTable =
                LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(getLibraryLevel(), getRootModel().getModule().getProject());
        if (libraryTable != null) {
            myProjectRootManagerImpl.removeListenerForTable(myLibraryListener, libraryTable);
        }
    }


    private void afterLibraryAdded( Library newLibrary) {
        if (myLibrary == null) {
            if (Comparing.equal(myLibraryName, newLibrary.getName())) {
                myLibrary = newLibrary;
                myLibraryName = null;
                myLibraryLevel = null;
                updateFromRootProviderAndSubscribe();
            }
        }
    }

    private void beforeLibraryRemoved(Library library) {
        if (library == myLibrary) {
            myLibraryName = myLibrary.getName();
            myLibraryLevel = myLibrary.getTable().getTableLevel();
            myLibrary = null;
            updateFromRootProviderAndSubscribe();
        }
    }

    private class MyOrderEntryLibraryTableListener implements LibraryTable.Listener {
        public MyOrderEntryLibraryTableListener() {
        }

        @Override
        public void afterLibraryAdded( Library newLibrary) {
            LibraryOrderEntryImpl.this.afterLibraryAdded(newLibrary);
        }

        @Override
        public void afterLibraryRenamed( Library library) {
            afterLibraryAdded(library);
        }

        @Override
        public void beforeLibraryRemoved(Library library) {
            LibraryOrderEntryImpl.this.beforeLibraryRemoved(library);
        }

        @Override
        public void afterLibraryRemoved(Library library) {
        }
    }
}
