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
package com.gome.maven.util.indexing;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.BaseComponent;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ContentIterator;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileWithId;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;
import com.gome.maven.util.SystemProperties;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Author: dmitrylomov
 */
public abstract class FileBasedIndex implements BaseComponent {
    public abstract void iterateIndexableFiles( ContentIterator processor,  Project project, ProgressIndicator indicator);

    public abstract void registerIndexableSet( IndexableFileSet set,  Project project);

    public abstract void removeIndexableSet( IndexableFileSet set);

    public static FileBasedIndex getInstance() {
        return ApplicationManager.getApplication().getComponent(FileBasedIndex.class);
    }

    public static int getFileId( final VirtualFile file) {
        if (file instanceof VirtualFileWithId) {
            return ((VirtualFileWithId)file).getId();
        }

        throw new IllegalArgumentException("Virtual file doesn't support id: " + file + ", implementation class: " + file.getClass().getName());
    }

    // note: upsource implementation requires access to Project here, please don't remove
    public abstract VirtualFile findFileById(Project project, int id);

    public void requestRebuild(ID<?, ?> indexId) {
        requestRebuild(indexId, new Throwable());
    }



    public String getComponentName() {
        return "FileBasedIndex";
    }

    
    public abstract <K, V> List<V> getValues( ID<K, V> indexId,  K dataKey,  GlobalSearchScope filter);

    
    public abstract <K, V> Collection<VirtualFile> getContainingFiles( ID<K, V> indexId,
                                                                       K dataKey,
                                                                       GlobalSearchScope filter);

    /**
     * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
     */
    public abstract <K, V> boolean processValues( ID<K, V> indexId,
                                                  K dataKey,
                                                  VirtualFile inFile,
                                                  FileBasedIndex.ValueProcessor<V> processor,
                                                  GlobalSearchScope filter);

    /**
     * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
     */
    public <K, V> boolean processValues( ID<K, V> indexId,
                                         K dataKey,
                                         VirtualFile inFile,
                                         FileBasedIndex.ValueProcessor<V> processor,
                                         GlobalSearchScope filter,
                                         IdFilter idFilter) {
        return processValues(indexId, dataKey, inFile, processor, filter);
    }

    public abstract <K, V> boolean processFilesContainingAllKeys( ID<K, V> indexId,
                                                                  Collection<K> dataKeys,
                                                                  GlobalSearchScope filter,
                                                                  Condition<V> valueChecker,
                                                                  Processor<VirtualFile> processor);

    /**
     * @param project it is guaranteed to return data which is up-to-date withing the project
     *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
     */
    
    public abstract <K> Collection<K> getAllKeys( ID<K, ?> indexId,  Project project);

    /**
     * DO NOT CALL DIRECTLY IN CLIENT CODE
     * The method is internal to indexing engine end is called internally. The method is public due to implementation details
     */
    public abstract <K> void ensureUpToDate( ID<K, ?> indexId,  Project project,  GlobalSearchScope filter);

    public abstract void requestRebuild(ID<?, ?> indexId, Throwable throwable);

    public abstract <K> void scheduleRebuild( ID<K, ?> indexId,  Throwable e);

    public abstract void requestReindex( VirtualFile file);

    public abstract <K, V> boolean getFilesWithKey( ID<K, V> indexId,
                                                    Set<K> dataKeys,
                                                    Processor<VirtualFile> processor,
                                                    GlobalSearchScope filter);

    /**
     * @param project it is guaranteed to return data which is up-to-date withing the project
     *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
     */
    public abstract <K> boolean processAllKeys( ID<K, ?> indexId,  Processor<K> processor,  Project project);

    public <K> boolean processAllKeys( ID<K, ?> indexId,  Processor<K> processor,  GlobalSearchScope scope,  IdFilter idFilter) {
        return processAllKeys(indexId, processor, scope.getProject());
    }

    public interface ValueProcessor<V> {
        /**
         * @param value a value to process
         * @param file the file the value came from
         * @return false if no further processing is needed, true otherwise
         */
        boolean process(VirtualFile file, V value);
    }

    /**
     * Author: dmitrylomov
     */
    public interface InputFilter {
        boolean acceptInput( VirtualFile file);
    }

    public interface FileTypeSpecificInputFilter extends InputFilter {
        void registerFileTypesUsedForIndexing( Consumer<FileType> fileTypeSink);
    }

    // TODO: remove once changes becomes permanent
    public static final boolean ourEnableTracingOfKeyHashToVirtualFileMapping =
            SystemProperties.getBooleanProperty("idea.enable.tracing.keyhash2virtualfile", true);
}
