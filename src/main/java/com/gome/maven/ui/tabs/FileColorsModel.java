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

package com.gome.maven.ui.tabs;

import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.packageDependencies.DefaultScopesProvider;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.scope.NonProjectFilesScope;
import com.gome.maven.psi.search.scope.TestsScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;
import com.gome.maven.psi.search.scope.packageSet.PackageSet;
import com.gome.maven.psi.search.scope.packageSet.PackageSetBase;
import com.gome.maven.ui.ColorUtil;
import com.gome.maven.util.PlatformUtils;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
// todo[spL]: listen to scope rename
public class FileColorsModel implements Cloneable {
    public static final String FILE_COLOR = "fileColor";

    private final List<FileColorConfiguration> myConfigurations;
    private final List<FileColorConfiguration> mySharedConfigurations;
    private static final Map<String, String> globalScopes;
    private static final Map<String, String> globalScopesColors;
    static {
        globalScopes = new HashMap<String, String>();
        globalScopes.put(NonProjectFilesScope.NAME, "file.colors.enable.non.project");
        if (PlatformUtils.isIntelliJ() || PlatformUtils.isRubyMine()) {
            globalScopes.put(TestsScope.NAME, "file.colors.enable.tests");
        }

        globalScopesColors = new HashMap<String, String>();
    }

    
    private final Project myProject;

    FileColorsModel( final Project project) {
        myProject = project;
        myConfigurations = new ArrayList<FileColorConfiguration>();
        mySharedConfigurations = new ArrayList<FileColorConfiguration>();

        if (globalScopesColors.size() < globalScopes.size()) {
            final DefaultScopesProvider defaultScopesProvider = DefaultScopesProvider.getInstance(project);
            for (String scopeName : globalScopes.keySet()) {
                final NamedScope scope = defaultScopesProvider.findCustomScope(scopeName);
                assert scope != null : "There is no custom scope with name " + scopeName;
                final Color color = ColorUtil.getColor(scope.getClass());
                assert color != null : scope.getClass().getName() + " is not annotated with @Colored";
                final String colorName = FileColorManagerImpl.getColorName(color);
                globalScopesColors.put(scopeName, colorName == null ? ColorUtil.toHex(color) : colorName);
            }
        }
        initGlobalScopes();
    }

    private FileColorsModel( final Project project,
                             final List<FileColorConfiguration> regular,
                             final List<FileColorConfiguration> shared) {
        myProject = project;
        myConfigurations = new ArrayList<FileColorConfiguration>();
        mySharedConfigurations = new ArrayList<FileColorConfiguration>();
        myConfigurations.addAll(regular);
        mySharedConfigurations.addAll(shared);
        initGlobalScopes();
    }

    private void initGlobalScopes() {
        for (String scopeName : globalScopes.keySet()) {
            if (findConfiguration(scopeName, false) == null) {
                final String color = PropertiesComponent.getInstance().getOrInit(globalScopes.get(scopeName), globalScopesColors.get(scopeName));
                if (color.length() != 0) {
                    final Color col = ColorUtil.fromHex(color, null);
                    final String name = col == null ? null : FileColorManagerImpl.getColorName(col);
                    myConfigurations.add(new FileColorConfiguration(scopeName, name == null ? color : name));
                }
            }
        }
    }

    public void save(final Element e, final boolean shared) {
        final List<FileColorConfiguration> configurations = shared ? mySharedConfigurations : myConfigurations;
        for (final FileColorConfiguration configuration : configurations) {
            final String name = configuration.getScopeName();
            if (!shared && globalScopes.containsKey(name)) {
                PropertiesComponent.getInstance().setValue(name, configuration.getColorName());
            } else {
                configuration.save(e);
            }
        }
    }

    public void load(final Element e, final boolean shared) {
        List<FileColorConfiguration> configurations = shared ? mySharedConfigurations : myConfigurations;

        configurations.clear();

        final List<Element> list = e.getChildren(FILE_COLOR);
        final Map<String, String> global = new HashMap<String, String>(globalScopes);
        for (Element child : list) {
            final FileColorConfiguration configuration = FileColorConfiguration.load(child);
            if (configuration != null) {
                if (!shared) {
                    final String name = configuration.getScopeName();
                    if (globalScopes.get(name) != null) {
                        global.remove(name);
                    }
                }
                configurations.add(configuration);
            }
        }

        if (!shared) {
            final PropertiesComponent properties = PropertiesComponent.getInstance();
            for (String scope : global.keySet()) {
                final String colorName = properties.getValue(scope);
                if (colorName != null) {
                    configurations.add(new FileColorConfiguration(scope, colorName));
                }
            }
        }
    }

    @Override
    public FileColorsModel clone() throws CloneNotSupportedException {
        final List<FileColorConfiguration> regular = new ArrayList<FileColorConfiguration>();
        for (final FileColorConfiguration configuration : myConfigurations) {
            regular.add(configuration.clone());
        }

        final ArrayList<FileColorConfiguration> shared = new ArrayList<FileColorConfiguration>();
        for (final FileColorConfiguration sharedConfiguration : mySharedConfigurations) {
            shared.add(sharedConfiguration.clone());
        }

        return new FileColorsModel(myProject, regular, shared);
    }

    public void add( final FileColorConfiguration configuration, boolean shared) {
        final List<FileColorConfiguration> configurations = shared ? mySharedConfigurations : myConfigurations;
        if (!configurations.contains(configuration)) {
            configurations.add(configuration);
        }
    }

    public void add( final String scopeName,  final String colorName, boolean shared) {
        final FileColorConfiguration configuration = new FileColorConfiguration();
        configuration.setScopeName(scopeName);
        configuration.setColorName(colorName);

        add(configuration, shared);
    }

    
    private FileColorConfiguration findConfiguration(final String scopeName, final boolean shared) {
        final List<FileColorConfiguration> configurations = shared ? mySharedConfigurations : myConfigurations;
        for (final FileColorConfiguration configuration : configurations) {
            if (scopeName.equals(configuration.getScopeName())) {
                return configuration;
            }
        }

        return null;
    }

    public boolean isShared( final String scopeName) {
        return findConfiguration(scopeName, true) != null;
    }

    
    public String getColor( PsiFile psiFile) {
        if (!psiFile.isValid()) {
            return null;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile == null? null : getColor(virtualFile, psiFile.getProject());
    }

    
    public String getColor( VirtualFile file, Project project) {
        if (!file.isValid()) {
            return null;
        }

        final FileColorConfiguration configuration = findConfiguration(file);
        if (configuration != null && configuration.isValid(project)) {
            return configuration.getColorName();
        }
        return null;
    }

    
    private FileColorConfiguration findConfiguration( final VirtualFile colored) {
        for (FileColorConfiguration configuration : ContainerUtil.concat(myConfigurations, mySharedConfigurations)) {
            NamedScope scope = NamedScopesHolder.getScope(myProject, configuration.getScopeName());
            if (scope != null) {
                NamedScopesHolder namedScopesHolder = NamedScopesHolder.getHolder(myProject, configuration.getScopeName(), null);
                PackageSet packageSet = scope.getValue();
                if (packageSet instanceof PackageSetBase && namedScopesHolder != null && ((PackageSetBase)packageSet).contains(colored, myProject, namedScopesHolder)) {
                    return configuration;
                }
            }
        }
        return null;
    }


    public boolean isShared(FileColorConfiguration configuration) {
        return mySharedConfigurations.contains(configuration);
    }

    public void setConfigurations(final List<FileColorConfiguration> configurations, final boolean shared) {
        if (shared) {
            mySharedConfigurations.clear();
            mySharedConfigurations.addAll(configurations);
        }
        else {
            myConfigurations.clear();
            final HashMap<String, String> global = new HashMap<String, String>(globalScopes);
            for (FileColorConfiguration configuration : configurations) {
                myConfigurations.add(configuration);
                final String name = configuration.getScopeName();
                if (global.containsKey(name)) {
                    PropertiesComponent.getInstance().setValue(global.get(name), configuration.getColorName());
                    global.remove(name);
                }
            }
            for (String name : global.keySet()) {
                PropertiesComponent.getInstance().setValue(global.get(name), "");
            }
        }
    }

    public boolean isColored(String scopeName, boolean shared) {
        return findConfiguration(scopeName, shared) != null;
    }

    public List<FileColorConfiguration> getLocalConfigurations() {
        return myConfigurations;
    }

    public List<FileColorConfiguration> getSharedConfigurations() {
        return mySharedConfigurations;
    }
}
