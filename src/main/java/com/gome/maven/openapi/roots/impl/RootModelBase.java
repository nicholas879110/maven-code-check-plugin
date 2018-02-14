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

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.*;

/**
 * @author nik
 */
public abstract class RootModelBase implements ModuleRootModel {
    @Override
    
    public VirtualFile[] getContentRoots() {
        final ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();

        for (ContentEntry contentEntry : getContent()) {
            final VirtualFile file = contentEntry.getFile();
            if (file != null) {
                result.add(file);
            }
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    @Override
    
    public String[] getContentRootUrls() {
        if (getContent().isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;
        final ArrayList<String> result = new ArrayList<String>(getContent().size());

        for (ContentEntry contentEntry : getContent()) {
            result.add(contentEntry.getUrl());
        }

        return ArrayUtil.toStringArray(result);
    }

    @Override
    
    public String[] getExcludeRootUrls() {
        final List<String> result = new SmartList<String>();
        for (ContentEntry contentEntry : getContent()) {
            result.addAll(contentEntry.getExcludeFolderUrls());
        }
        return ArrayUtil.toStringArray(result);
    }

    @Override
    
    public VirtualFile[] getExcludeRoots() {
        final List<VirtualFile> result = new SmartList<VirtualFile>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getExcludeFolderFiles());
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    @Override
    
    public String[] getSourceRootUrls() {
        return getSourceRootUrls(true);
    }

    @Override
    
    public String[] getSourceRootUrls(boolean includingTests) {
        List<String> result = new SmartList<String>();
        for (ContentEntry contentEntry : getContent()) {
            final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : sourceFolders) {
                if (includingTests || !sourceFolder.isTestSource()) {
                    result.add(sourceFolder.getUrl());
                }
            }
        }
        return ArrayUtil.toStringArray(result);
    }

    @Override
    
    public VirtualFile[] getSourceRoots() {
        return getSourceRoots(true);
    }

    @Override
    
    public VirtualFile[] getSourceRoots(final boolean includingTests) {
        List<VirtualFile> result = new SmartList<VirtualFile>();
        for (ContentEntry contentEntry : getContent()) {
            final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : sourceFolders) {
                final VirtualFile file = sourceFolder.getFile();
                if (file != null && (includingTests || !sourceFolder.isTestSource())) {
                    result.add(file);
                }
            }
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    
    @Override
    public List<VirtualFile> getSourceRoots( JpsModuleSourceRootType<?> rootType) {
        return getSourceRoots(Collections.singleton(rootType));
    }

    
    @Override
    public List<VirtualFile> getSourceRoots( Set<? extends JpsModuleSourceRootType<?>> rootTypes) {
        List<VirtualFile> result = new SmartList<VirtualFile>();
        for (ContentEntry contentEntry : getContent()) {
            final List<SourceFolder> sourceFolders = contentEntry.getSourceFolders(rootTypes);
            for (SourceFolder sourceFolder : sourceFolders) {
                final VirtualFile file = sourceFolder.getFile();
                if (file != null) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    
    @Override
    public ContentEntry[] getContentEntries() {
        final Collection<ContentEntry> content = getContent();
        return content.toArray(new ContentEntry[content.size()]);
    }

    protected abstract Collection<ContentEntry> getContent();

    @Override
    public Sdk getSdk() {
        for (OrderEntry orderEntry : getOrderEntries()) {
            if (orderEntry instanceof JdkOrderEntry) {
                return ((JdkOrderEntry)orderEntry).getJdk();
            }
        }
        return null;
    }

    @Override
    public boolean isSdkInherited() {
        for (OrderEntry orderEntry : getOrderEntries()) {
            if (orderEntry instanceof InheritedJdkOrderEntry) {
                return true;
            }
        }
        return false;
    }

    
    @Override
    public OrderEnumerator orderEntries() {
        return new ModuleOrderEnumerator(this, null);
    }

    @Override
    public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
        R result = initialValue;
        for (OrderEntry orderEntry : getOrderEntries()) {
            result = orderEntry.accept(policy, result);
        }
        return result;
    }

    @Override
    
    public String[] getDependencyModuleNames() {
        List<String> result = orderEntries().withoutSdk().withoutLibraries().withoutModuleSourceEntries()
                .process(new CollectDependentModules(), new ArrayList<String>());
        return ArrayUtil.toStringArray(result);
    }

    @Override
    
    public Module[] getModuleDependencies() {
        return getModuleDependencies(true);
    }

    @Override
    
    public Module[] getModuleDependencies(boolean includeTests) {
        final List<Module> result = new ArrayList<Module>();

        for (OrderEntry entry : getOrderEntries()) {
            if (entry instanceof ModuleOrderEntry) {
                ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)entry;
                final DependencyScope scope = moduleOrderEntry.getScope();
                if (!includeTests && !scope.isForProductionCompile() && !scope.isForProductionRuntime()) {
                    continue;
                }
                final Module module1 = moduleOrderEntry.getModule();
                if (module1 != null) {
                    result.add(module1);
                }
            }
        }

        return result.isEmpty() ? Module.EMPTY_ARRAY : ContainerUtil.toArray(result, new Module[result.size()]);
    }

    private static class CollectDependentModules extends RootPolicy<List<String>> {
        
        @Override
        public List<String> visitModuleOrderEntry( ModuleOrderEntry moduleOrderEntry,  List<String> arrayList) {
            arrayList.add(moduleOrderEntry.getModuleName());
            return arrayList;
        }
    }
}
