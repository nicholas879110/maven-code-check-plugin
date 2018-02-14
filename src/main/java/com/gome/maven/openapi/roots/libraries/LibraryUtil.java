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

/**
 * @author cdr
 */
package com.gome.maven.openapi.roots.libraries;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashSet;
import com.gome.maven.util.text.StringTokenizer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LibraryUtil {
    private LibraryUtil() {
    }

    public static boolean isClassAvailableInLibrary(final Library library, final String fqn) {
        return isClassAvailableInLibrary(library.getFiles(OrderRootType.CLASSES), fqn);
    }

    public static boolean isClassAvailableInLibrary(VirtualFile[] files, final String fqn) {
        return isClassAvailableInLibrary(Arrays.asList(files), fqn);
    }

    public static boolean isClassAvailableInLibrary(List<VirtualFile> files, final String fqn) {
        for (VirtualFile file : files) {
            if (findInFile(file, new StringTokenizer(fqn, "."))) return true;
        }
        return false;
    }

    
    public static Library findLibraryByClass(final String fqn,  Project project) {
        if (project != null) {
            final LibraryTable projectTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
            Library library = findInTable(projectTable, fqn);
            if (library != null) {
                return library;
            }
        }
        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable();
        return findInTable(table, fqn);
    }


    private static boolean findInFile(VirtualFile file, final StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) return true;
         StringBuilder name = new StringBuilder(tokenizer.nextToken());
        if (!tokenizer.hasMoreTokens()) {
            name.append(".class");
        }
        final VirtualFile child = file.findChild(name.toString());
        return child != null && findInFile(child, tokenizer);
    }

    
    private static Library findInTable(LibraryTable table, String fqn) {
        for (Library library : table.getLibraries()) {
            if (isClassAvailableInLibrary(library, fqn)) {
                return library;
            }
        }
        return null;
    }

    public static Library createLibrary(final LibraryTable libraryTable,  final String baseName) {
        String name = baseName;
        int count = 2;
        while (libraryTable.getLibraryByName(name) != null) {
            name = baseName + " (" + count++ + ")";
        }
        return libraryTable.createLibrary(name);
    }

    public static VirtualFile[] getLibraryRoots(final Project project) {
        return getLibraryRoots(project, true, true);
    }

    public static VirtualFile[] getLibraryRoots(final Project project, final boolean includeSourceFiles, final boolean includeJdk) {
        return getLibraryRoots(ModuleManager.getInstance(project).getModules(), includeSourceFiles, includeJdk);
    }

    public static VirtualFile[] getLibraryRoots(final Module[] modules, final boolean includeSourceFiles, final boolean includeJdk) {
        Set<VirtualFile> roots = new HashSet<VirtualFile>();
        for (Module module : modules) {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            for (OrderEntry entry : orderEntries) {
                if (entry instanceof LibraryOrderEntry){
                    final Library library = ((LibraryOrderEntry)entry).getLibrary();
                    if (library != null) {
                        VirtualFile[] files = includeSourceFiles ? library.getFiles(OrderRootType.SOURCES) : null;
                        if (files == null || files.length == 0) {
                            files = library.getFiles(OrderRootType.CLASSES);
                        }
                        ContainerUtil.addAll(roots, files);
                    }
                } else if (includeJdk && entry instanceof JdkOrderEntry) {
                    JdkOrderEntry jdkEntry = (JdkOrderEntry)entry;
                    VirtualFile[] files = includeSourceFiles ? jdkEntry.getRootFiles(OrderRootType.SOURCES) : null;
                    if (files == null || files.length == 0) {
                        files = jdkEntry.getRootFiles(OrderRootType.CLASSES);
                    }
                    ContainerUtil.addAll(roots, files);
                }
            }
        }
        return VfsUtilCore.toVirtualFileArray(roots);
    }

    
    public static Library findLibrary( Module module,  final String name) {
        final Ref<Library> result = Ref.create(null);
        OrderEnumerator.orderEntries(module).forEachLibrary(new Processor<Library>() {
            @Override
            public boolean process(Library library) {
                if (name.equals(library.getName())) {
                    result.set(library);
                    return false;
                }
                return true;
            }
        });
        return result.get();
    }

    
    public static OrderEntry findLibraryEntry(VirtualFile file, final Project project) {
        List<OrderEntry> entries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(file);
        for (OrderEntry entry : entries) {
            if (entry instanceof LibraryOrderEntry || entry instanceof JdkOrderEntry) {
                return entry;
            }
        }
        return null;
    }
}
