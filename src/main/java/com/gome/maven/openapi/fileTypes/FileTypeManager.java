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
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.CachedSingletonsRegistry;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.messages.Topic;
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the relationship between filenames and {@link FileType} instances.
 */

public abstract class FileTypeManager extends FileTypeRegistry {
    static {
        FileTypeRegistry.ourInstanceGetter = new Getter<FileTypeRegistry>() {
            @Override
            public FileTypeRegistry get() {
                return FileTypeManager.getInstance();
            }
        };
    }

    private static FileTypeManager ourInstance = CachedSingletonsRegistry.markCachedField(FileTypeManager.class);

    
    public static final Topic<FileTypeListener> TOPIC = new Topic<FileTypeListener>("File types change", FileTypeListener.class);

    /**
     * Returns the singleton instance of the FileTypeManager component.
     *
     * @return the instance of FileTypeManager
     */
    public static FileTypeManager getInstance() {
        FileTypeManager instance = ourInstance;
        if (instance == null) {
            Application app = ApplicationManager.getApplication();
            ourInstance = instance = app != null ? app.getComponent(FileTypeManager.class) : new MockFileTypeManager();
        }
        return instance;
    }

    /**
     * @deprecated use {@link com.gome.maven.openapi.fileTypes.FileTypeFactory} instead
     */
    public abstract void registerFileType( FileType type,  List<FileNameMatcher> defaultAssociations);

    /**
     * Registers a file type.
     *
     * @param type                        The file type to register.
     * @param defaultAssociatedExtensions The list of extensions which cause the file to be
     *                                    treated as the specified file type. The extensions should not start with '.'.
     * @deprecated use {@link com.gome.maven.openapi.fileTypes.FileTypeFactory} instead
     */
    public final void registerFileType( FileType type,  String... defaultAssociatedExtensions) {
        List<FileNameMatcher> matchers = new ArrayList<FileNameMatcher>();
        if (defaultAssociatedExtensions != null) {
            for (String extension : defaultAssociatedExtensions) {
                matchers.add(new ExtensionFileNameMatcher(extension));
            }
        }
        registerFileType(type, matchers);
    }

    /**
     * Checks if the specified file is ignored by IDEA. Ignored files are not visible in
     * different project views and cannot be opened in the editor. They will neither be parsed nor compiled.
     *
     * @param name The name of the file to check.
     * @return true if the file is ignored, false otherwise.
     */

    public abstract boolean isFileIgnored(  String name);

    /**
     * Returns the list of extensions associated with the specified file type.
     *
     * @param type The file type for which the extensions are requested.
     * @return The array of extensions associated with the file type.
     * @deprecated since more generic way of associations by means of wildcards exist not every associations matches extension paradigm
     */
    
    public abstract String[] getAssociatedExtensions( FileType type);


    
    public abstract List<FileNameMatcher> getAssociations( FileType type);

    public abstract boolean isFileOfType( VirtualFile file,  FileType type);

    /**
     * Adds a listener for receiving notifications about changes in the list of
     * registered file types.
     *
     * @param listener The listener instance.
     * @deprecated Subscribe to {@link #TOPIC} on any message bus level instead.
     */

    public abstract void addFileTypeListener( FileTypeListener listener);

    /**
     * Removes a listener for receiving notifications about changes in the list of
     * registered file types.
     *
     * @param listener The listener instance.
     * @deprecated Subscribe to {@link #TOPIC} on any message bus level instead.
     */

    public abstract void removeFileTypeListener( FileTypeListener listener);

    /**
     * If fileName is already associated with any known file type returns it.
     * Otherwise asks user to select file type and associates it with fileName extension if any selected.
     *
     * @param file - a file to ask for file type association
     * @return Known file type or null. Never returns {@link FileTypes#UNKNOWN}.
     */
    
    @Deprecated() // use getKnownFileTypeOrAssociate(VirtualFile file, Project project) instead
    public abstract FileType getKnownFileTypeOrAssociate( VirtualFile file);
    
    public abstract FileType getKnownFileTypeOrAssociate( VirtualFile file,  Project project);

    /**
     * Returns the semicolon-delimited list of patterns for files and folders
     * which are excluded from the project structure though they may be present
     * physically on the HD.
     *
     * @return Semicolon-delimited list of patterns.
     */
    
    public abstract String getIgnoredFilesList();

    /**
     * Sets new list of semicolon-delimited patterns for files and folders which
     * are excluded from the project structure.
     *
     * @param list List of semicolon-delimited patterns.
     */
    public abstract void setIgnoredFilesList( String list);

    /**
     * Adds an extension to the list of extensions associated with a file type.
     *
     * @param type      the file type to associate the extension with.
     * @param extension the extension to associate.
     * @since 5.0.2
     */
    public final void associateExtension( FileType type,   String extension) {
        associate(type, new ExtensionFileNameMatcher(extension));
    }

    public final void associatePattern( FileType type,   String pattern) {
        associate(type, parseFromString(pattern));
    }

    public abstract void associate( FileType type,  FileNameMatcher matcher);

    /**
     * Removes an extension from the list of extensions associated with a file type.
     *
     * @param type      the file type to remove the extension from.
     * @param extension the extension to remove.
     * @since 5.0.2
     */
    public final void removeAssociatedExtension( FileType type,   String extension) {
        removeAssociation(type, new ExtensionFileNameMatcher(extension));
    }

    public abstract void removeAssociation( FileType type,  FileNameMatcher matcher);

    
    public static FileNameMatcher parseFromString( String pattern) {
        return FileNameMatcherFactory.getInstance().createMatcher(pattern);
    }

    
    public abstract FileType getStdFileType(  String fileTypeName);
}
