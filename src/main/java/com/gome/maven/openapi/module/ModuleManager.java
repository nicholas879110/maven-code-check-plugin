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
package com.gome.maven.openapi.module;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.SimpleModificationTracker;
import com.gome.maven.util.graph.Graph;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Provides services for working with the modules of a project.
 */
public abstract class ModuleManager extends SimpleModificationTracker {
    /**
     * Returns the module manager instance for the current project.
     *
     * @param project the project for which the module manager is requested.
     * @return the module manager instance.
     */
    public static ModuleManager getInstance( Project project) {
        return project.getComponent(ModuleManager.class);
    }

    /**
     * Creates a module of the specified type at the specified path and adds it to the project
     * to which the module manager is related.
     *
     * @param filePath the path at which the module is created.
     * @param moduleTypeId the ID of the module type to create.
     * @return the module instance.
     */
     public abstract Module newModule(  String filePath, final String moduleTypeId);

    /**
     * Loads a module from an .iml file with the specified path and adds it to the project.
     *
     * @param filePath the path to load the module from.
     * @return the module instance.
     * @throws InvalidDataException if the data in the .iml file is semantically incorrect.
     * @throws IOException if an I/O error occurred when loading the module file.
     * @throws JDOMException if the file contains invalid XML data.
     * @throws ModuleWithNameAlreadyExists if a module with such a name already exists in the project.
     */
     public abstract Module loadModule( String filePath) throws InvalidDataException, IOException, JDOMException, ModuleWithNameAlreadyExists;

    /**
     * Disposes of the specified module and removes it from the project.
     *
     * @param module the module to remove.
     */
    public abstract void disposeModule( Module module);

    /**
     * Returns the list of all modules in the project.
     *
     * @return the array of modules.
     */
     public abstract Module[] getModules();

    /**
     * Returns the project module with the specified name.
     *
     * @param name the name of the module to find.
     * @return the module instance, or null if no module with such name exists.
     */
     public abstract Module findModuleByName(  String name);

    /**
     * Returns the list of modules sorted by dependency (the modules which do not depend
     * on anything are in the beginning of the list, a module which depends on another module
     * follows it in the list).
     *
     * @return the sorted array of modules.
     */
     public abstract Module[] getSortedModules();

    /**
     * Returns the module comparator which can be used for sorting modules by dependency
     * (the modules which do not depend on anything are in the beginning of the list,
     * a module which depends on another module follows it in the list).
     *
     * @return the module comparator instance.
     */
     public abstract Comparator<Module> moduleDependencyComparator();

    /**
     * Returns the list of modules which directly depend on the specified module.
     *
     * @param module the module for which the list of dependent modules is requested.
     * @return list of <i>modules that depend on</i> given module.
     *
     * @see ModuleUtilCore#getAllDependentModules(Module)
     */
     public abstract List<Module> getModuleDependentModules( Module module);

    /**
     * Checks if one of the specified modules directly depends on the other module.
     *
     * @param module   the module to check the dependency for.
     * @param onModule the module on which <code>module</code> may depend.
     * @return true if <code>module</code> directly depends on <code>onModule</code>, false otherwise.
     */
    public abstract boolean isModuleDependent( Module module,  Module onModule);

    /**
     * Returns the graph of dependencies between modules in the project.
     *
     * @return the module dependency graph.
     */
     public abstract Graph<Module> moduleGraph();

    /**
     * Returns the graph of dependencies between modules in the project.
     *
     * @param includeTests whether test-only dependencies should be included
     * @return the module dependency graph.
     * @since 11.0
     */
     public abstract Graph<Module> moduleGraph(boolean includeTests);

    /**
     * Returns the model for the list of modules in the project, which can be used to add,
     * remove or modify modules.
     *
     * @return the modifiable model instance.
     */
     public abstract ModifiableModuleModel getModifiableModel();


    /**
     * Returns the path to the group to which the specified module belongs, as an
     * array of group names starting from the project root.
     *
     * @param module the module for which the path is requested.
     * @return the path to the group for the module, or null if the module does not belong to any group.
     */
     public abstract String[] getModuleGroupPath( Module module);
}
