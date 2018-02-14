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

import com.gome.maven.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.List;
import java.util.Set;

/**
 * Represents a module's content root.
 * You can get existing entries with {@link com.gome.maven.openapi.roots.ModuleRootModel#getContentEntries()} or
 * create a new one with {@link ModifiableRootModel#addContentEntry(com.gome.maven.openapi.vfs.VirtualFile)}.
 *
 * @author dsl
 * @see ModuleRootModel#getContentEntries()
 * @see ModifiableRootModel#addContentEntry(com.gome.maven.openapi.vfs.VirtualFile)
 */
public interface ContentEntry extends Synthetic {
    /**
     * Returns the root file or directory for the content root, if it is valid.
     *
     * @return the content root file or directory, or null if content entry is invalid.
     */
    
    VirtualFile getFile();

    /**
     * Returns the URL of content root.
     * To validate returned roots, use
     * <code>{@link com.gome.maven.openapi.vfs.VirtualFileManager#findFileByUrl(String)}</code>
     *
     * @return URL of content root, that should never be null.
     */
    
    String getUrl();

    /**
     * Returns the list of source roots under this content root.
     *
     * @return list of this <code>ContentEntry</code> {@link com.gome.maven.openapi.roots.SourceFolder}s
     */
    
    SourceFolder[] getSourceFolders();

    /**
     * @param rootType type of accepted source roots
     * @return list of source roots of the specified type containing in this content root
     */
    
    List<SourceFolder> getSourceFolders( JpsModuleSourceRootType<?> rootType);

    /**
     *
     * @param rootTypes types of accepted source roots
     * @return list of source roots of the specified types containing in this content root
     */
    
    List<SourceFolder> getSourceFolders( Set<? extends JpsModuleSourceRootType<?>> rootTypes);

    /**
     * Returns the list of files and directories for valid source roots under this content root.
     *
     * @return list of all valid source roots.
     */
    
    VirtualFile[] getSourceFolderFiles();

    /**
     * Returns the list of excluded roots configured under this content root. The result doesn't include synthetic excludes like the module output.
     *
     * @return list of this <code>ContentEntry</code> {@link com.gome.maven.openapi.roots.ExcludeFolder}s
     */
    
    ExcludeFolder[] getExcludeFolders();

    /**
     * @return list of URLs for all excluded roots under this content root including synthetic excludes like the module output
     */
    
    List<String> getExcludeFolderUrls();

    /**
     * Returns the list of files and directories for valid excluded roots under this content root.
     *
     * @return list of all valid exclude roots including synthetic excludes like the module output
     */
    
    VirtualFile[] getExcludeFolderFiles();

    /**
     * Adds a source or test source root under the content root.
     *
     * @param file         the file or directory to add as a source root.
     * @param isTestSource true if the file or directory is added as a test source root.
     * @return the object representing the added root.
     */
    
    SourceFolder addSourceFolder( VirtualFile file, boolean isTestSource);

    /**
     * Adds a source or test source root with the specified package prefix under the content root.
     *
     * @param file          the file or directory to add as a source root.
     * @param isTestSource  true if the file or directory is added as a test source root.
     * @param packagePrefix the package prefix for the root to add, or an empty string if no
     *                      package prefix is required.
     * @return the object representing the added root.
     */
    
    SourceFolder addSourceFolder( VirtualFile file, boolean isTestSource,  String packagePrefix);

    
    <P extends JpsElement>
    SourceFolder addSourceFolder( VirtualFile file,  JpsModuleSourceRootType<P> type,  P properties);

    
    <P extends JpsElement>
    SourceFolder addSourceFolder( VirtualFile file,  JpsModuleSourceRootType<P> type);

    /**
     * Adds a source or test source root under the content root.
     *
     * @param  url the file or directory url to add as a source root.
     * @param isTestSource true if the file or directory is added as a test source root.
     * @return the object representing the added root.
     */
    
    SourceFolder addSourceFolder( String url, boolean isTestSource);

    
    <P extends JpsElement>
    SourceFolder addSourceFolder( String url,  JpsModuleSourceRootType<P> type);

    
    <P extends JpsElement>
    SourceFolder addSourceFolder( String url,  JpsModuleSourceRootType<P> type,   P properties);

    /**
     * Removes a source or test source root from this content root.
     *
     * @param sourceFolder the source root to remove (must belong to this content root).
     */
    void removeSourceFolder( SourceFolder sourceFolder);

    void clearSourceFolders();

    /**
     * Adds an exclude root under the content root.
     *
     * @param file the file or directory to add as an exclude root.
     * @return the object representing the added root.
     */
    ExcludeFolder addExcludeFolder( VirtualFile file);

    /**
     * Adds an exclude root under the content root.
     *
     * @param url the file or directory url to add as an exclude root.
     * @return the object representing the added root.
     */
    ExcludeFolder addExcludeFolder( String url);

    /**
     * Removes an exclude root from this content root.
     *
     * @param excludeFolder the exclude root to remove (must belong to this content root).
     */
    void removeExcludeFolder( ExcludeFolder excludeFolder);

    /**
     * Removes an exclude root from this content root.
     * @param url url of the exclude root
     * @return {@code true} if the exclude root was removed
     */
    boolean removeExcludeFolder( String url);

    void clearExcludeFolders();
}
