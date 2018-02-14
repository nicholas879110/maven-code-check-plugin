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
package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.LibraryTable;
import com.gome.maven.openapi.vfs.VirtualFile;


/**
 * Model of roots that should be used by clients to modify module roots.
 *
 * @author dsl
 * @see ModuleRootManager#getModifiableModel()
 */
public interface ModifiableRootModel extends ModuleRootModel {

    Project getProject();

    /**
     * Adds the specified file or directory as a content root.
     *
     * @param root root of a content
     * @return new content entry
     */
    
    ContentEntry addContentEntry( VirtualFile root);

    /**
     * Adds the specified file or directory as a content root.
     *
     * @param url root of a content
     * @return new content entry
     */
    
    ContentEntry addContentEntry( String url);

    /**
     * Remove the specified content root.
     *
     * @param entry the content root to remove.
     */
    void removeContentEntry( ContentEntry entry);

    /**
     * Appends an order entry to the classpath.
     *
     * @param orderEntry the order entry to add.
     */
    void addOrderEntry( OrderEntry orderEntry);

    /**
     * Creates an entry for a given library and adds it to order
     *
     * @param library the library for which the entry is created.
     * @return newly created order entry for the library
     */
    
    LibraryOrderEntry addLibraryEntry( Library library);

    /**
     * Adds an entry for invalid library.
     *
     * @param name
     * @param level
     * @return
     */
    
    LibraryOrderEntry addInvalidLibrary(  String name,  String level);

    
    ModuleOrderEntry addModuleOrderEntry( Module module);

    
    ModuleOrderEntry addInvalidModuleEntry( String name);

    
    LibraryOrderEntry findLibraryOrderEntry( Library library);

    /**
     * Removes order entry from an order.
     *
     * @param orderEntry
     */
    void removeOrderEntry( OrderEntry orderEntry);

    /**
     * @param newOrder
     */
    void rearrangeOrderEntries( OrderEntry[] newOrder);

    void clear();

    /**
     * Commits changes to a <code>{@link ModuleRootManager}</code>.
     * Should be invoked in a write action. After <code>commit()<code>, the model
     * becomes read-only.
     *
     * Use of ModuleRootModificationUtil.updateModel() is recommended.
     */
    void commit();

    /**
     * Must be invoked for uncommitted models that are no longer needed.
     *
     * Use of ModuleRootModificationUtil.updateModel() is recommended.
     */
    void dispose();

    /**
     * Returns library table with module libraries.<br>
     * <b>Note:</b> returned library table does not support listeners.
     *
     * @return library table to be modified
     */
    
    LibraryTable getModuleLibraryTable();

    /**
     * Sets JDK for this module to a specific value
     *
     * @param jdk
     */
    void setSdk( Sdk jdk);

    /**
     * Sets JDK name and type for this module.
     * To be used when JDK with this name and type does not exist (e.g. when importing module configuration).
     *
     * @param sdkName JDK name
     * @param sdkType JDK type
     */
    void setInvalidSdk( String sdkName, String sdkType);

    /**
     * Makes this module inheriting JDK from its project
     */
    void inheritSdk();

    boolean isChanged();

    boolean isWritable();

    <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, T entry);

    
    String getSdkName();

    boolean isDisposed();
}