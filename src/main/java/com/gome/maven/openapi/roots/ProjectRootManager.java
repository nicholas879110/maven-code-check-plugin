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
package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.util.SimpleModificationTracker;
import com.gome.maven.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Allows to query and modify the list of root files and directories belonging to a project.
 */
public abstract class ProjectRootManager extends SimpleModificationTracker {
    /**
     * Returns the project root manager instance for the specified project.
     *
     * @param project the project for which the instance is requested.
     * @return the instance.
     */
    public static ProjectRootManager getInstance( Project project) {
        final ProjectRootManager service = ServiceManager.getService(project, ProjectRootManager.class);
        if (service != null) return service;
        return project.getComponent(ProjectRootManager.class);
    }

    /**
     * Returns the file index for the project.
     *
     * @return the file index instance.
     */
    
    public abstract ProjectFileIndex getFileIndex();

    /**
     * Creates new enumerator instance to process dependencies of all modules in the project. Only first level dependencies of
     * modules are processed so {@link OrderEnumerator#recursively()} option is ignored and {@link OrderEnumerator#withoutDepModules()} option is forced
     * @return new enumerator instance
     */
    
    public abstract OrderEnumerator orderEntries();

    /**
     * Creates new enumerator instance to process dependencies of several modules in the project. Caching is not supported for this enumerator
     * @param modules modules to process
     * @return new enumerator instance
     */
    
    public abstract OrderEnumerator orderEntries( Collection<? extends Module> modules);

    /**
     * Unlike getContentRoots(), this includes the project base dir. Is this really necessary?
     * TODO: remove this method?
     */
    public abstract VirtualFile[] getContentRootsFromAllModules();

    /**
     * Returns the list of content root URLs for all modules in the project.
     *
     * @return the list of content root URLs.
     */
    
    public abstract List<String> getContentRootUrls();

    /**
     * Returns the list of content roots for all modules in the project.
     *
     * @return the list of content roots.
     */
    
    public abstract VirtualFile[] getContentRoots();

    /**
     * Returns the list of source roots under the content roots for all modules in the project.
     *
     * @return the list of content source roots.
     */
    
    public abstract VirtualFile[] getContentSourceRoots();

    /**
     * Returns the list of source roots from all modules which types belong to the specified set
     *
     * @param rootTypes types of source roots
     * @return list of source roots
     */
    
    public abstract List<VirtualFile> getModuleSourceRoots( Set<? extends JpsModuleSourceRootType<?>> rootTypes);

    /**
     * Returns the instance of the JDK selected for the project.
     *
     * @return the JDK instance, or null if the name of the selected JDK does not correspond
     * to any existing JDK instance.
     */
    public abstract Sdk getProjectSdk();

    /**
     * Returns the name of the SDK selected for the project.
     *
     * @return the SDK name.
     */
    public abstract String getProjectSdkName();

    /**
     * Sets the SDK to be used for the project.
     *
     * @param sdk the SDK instance.
     */
    public abstract void setProjectSdk( Sdk sdk);


    /**
     * Sets the name of the JDK to be used for the project.
     *
     * @param name the name of the JDK.
     */
    public abstract void setProjectSdkName(String name);
}
