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

import com.gome.maven.ProjectTopics;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.caches.FileContent;
import com.gome.maven.ide.startup.impl.StartupManagerImpl;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.CacheUpdateRunner;
import com.gome.maven.openapi.project.DumbModeTask;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.CollectingContentIterator;
import com.gome.maven.openapi.roots.ModuleRootAdapter;
import com.gome.maven.openapi.roots.ModuleRootEvent;
import com.gome.maven.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since Jan 29, 2008
 */
public class UnindexedFilesUpdater extends DumbModeTask {
    private static final Logger LOG = Logger.getInstance("#com.intellij.util.indexing.UnindexedFilesUpdater");

    private final FileBasedIndexImpl myIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    private final Project myProject;
    private final boolean myOnStartup;

    public UnindexedFilesUpdater(final Project project, boolean onStartup) {
        myProject = project;
        myOnStartup = onStartup;
        project.getMessageBus().connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                DumbService.getInstance(project).cancelTask(UnindexedFilesUpdater.this);
            }
        });
    }

    private void updateUnindexedFiles(ProgressIndicator indicator) {
        PushedFilePropertiesUpdater.getInstance(myProject).pushAllPropertiesNow();

        indicator.setIndeterminate(true);
        indicator.setText(IdeBundle.message("progress.indexing.scanning"));

        CollectingContentIterator finder = myIndex.createContentIterator(indicator);
        long l = System.currentTimeMillis();
        myIndex.iterateIndexableFiles(finder, myProject, indicator);
        myIndex.filesUpdateEnumerationFinished();

        LOG.info("Indexable files iterated in " + (System.currentTimeMillis() - l) + " ms");
        List<VirtualFile> files = finder.getFiles();

        if (myOnStartup && !ApplicationManager.getApplication().isUnitTestMode()) {
            // full VFS refresh makes sense only after it's loaded, i.e. after scanning files to index is finished
            ((StartupManagerImpl)StartupManager.getInstance(myProject)).scheduleInitialVfsRefresh();
        }

        if (files.isEmpty()) {
            return;
        }

        long started = System.currentTimeMillis();
        LOG.info("Unindexed files update started: " + files.size() + " files to update");

        indicator.setIndeterminate(false);
        indicator.setText(IdeBundle.message("progress.indexing.updating"));

        indexFiles(indicator, files);
        LOG.info("Unindexed files update done in " + (System.currentTimeMillis() - started) + " ms");
    }

    private void indexFiles(ProgressIndicator indicator, List<VirtualFile> files) {
        CacheUpdateRunner.processFiles(indicator, true, files, myProject, new Consumer<FileContent>() {
            @Override
            public void consume(FileContent content) {
                try {
                    myIndex.indexFileContent(myProject, content);
                }
                finally {
                    IndexingStamp.flushCache(content.getVirtualFile());
                }
            }
        });
    }

    @Override
    public void performInDumbMode( ProgressIndicator indicator) {
        myIndex.filesUpdateStarted(myProject);
        try {
            updateUnindexedFiles(indicator);
        }
        catch (ProcessCanceledException e) {
            LOG.info("Unindexed files update canceled");
            throw e;
        } finally {
            myIndex.filesUpdateFinished(myProject);
        }
    }
}
