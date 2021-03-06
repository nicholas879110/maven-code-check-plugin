/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.compiler.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.compiler.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ThrowableRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is an adapter for running any FileProcessingCompiler as a compiler task.
 *
 * @author Eugene Zhuravlev
 *         Date: 9/5/12
 */
public class FileProcessingCompilerAdapterTask implements CompileTask {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.compiler.impl.FileProcessingCompilerAdapterTask");

    private final FileProcessingCompiler myCompiler;

    public FileProcessingCompilerAdapterTask(FileProcessingCompiler compiler) {
        myCompiler = compiler;
    }

    public FileProcessingCompiler getCompiler() {
        return myCompiler;
    }

    @Override
    public boolean execute(CompileContext context) {
        final Project project = context.getProject();

        try {
            final FileProcessingCompiler.ProcessingItem[] items = myCompiler.getProcessingItems(context);
            if (items.length == 0) {
                return true;
            }

            final List<FileProcessingCompiler.ProcessingItem> toProcess = new ArrayList<FileProcessingCompiler.ProcessingItem>();
            final Ref<IOException> ex = new Ref<IOException>(null);
            final FileProcessingCompilerStateCache cache = getCache(context);
            final boolean isMake = context.isMake();
            DumbService.getInstance(project).runReadActionInSmartMode(new Runnable() {
                public void run() {
                    try {
                        for (FileProcessingCompiler.ProcessingItem item : items) {
                            final VirtualFile file = item.getFile();
                            final String url = file.getUrl();
                            if (isMake && cache.getTimestamp(url) == file.getTimeStamp()) {
                                final ValidityState state = cache.getExtState(url);
                                final ValidityState itemState = item.getValidityState();
                                if (state != null ? state.equalsTo(itemState) : itemState == null) {
                                    continue;
                                }
                            }
                            toProcess.add(item);
                        }
                    }
                    catch (IOException e) {
                        ex.set(e);
                    }
                }
            });

            IOException exception = ex.get();
            if (exception != null) {
                throw exception;
            }

            if (toProcess.isEmpty()) {
                return true;
            }

            final FileProcessingCompiler.ProcessingItem[] array = toProcess.toArray(new FileProcessingCompiler.ProcessingItem[toProcess.size()]);
            final FileProcessingCompiler.ProcessingItem[] processed = myCompiler.process(context, array);
            if (processed.length == 0) {
                return true;
            }

            CompilerUtil.runInContext(context, CompilerBundle.message("progress.updating.caches"), new ThrowableRunnable<IOException>() {
                public void run() throws IOException{
                    final List<VirtualFile> vFiles = new ArrayList<VirtualFile>(processed.length);
                    final List<Pair<FileProcessingCompiler.ProcessingItem, ValidityState>> toUpdate = new ArrayList<Pair<FileProcessingCompiler.ProcessingItem, ValidityState>>(processed.length);
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            for (FileProcessingCompiler.ProcessingItem item : processed) {
                                vFiles.add(item.getFile());
                                toUpdate.add(Pair.create(item, item.getValidityState()));
                            }
                        }
                    });
                    LocalFileSystem.getInstance().refreshFiles(vFiles);

                    for (Pair<FileProcessingCompiler.ProcessingItem, ValidityState> pair : toUpdate) {
                        cache.update(pair.getFirst().getFile(), pair.getSecond());
                    }
                }
            });
        }
        catch (IOException e) {
            context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
            LOG.info(e);
        }
        return true;
    }

    private FileProcessingCompilerStateCache getCache(CompileContext context) throws IOException {
        final CompilerCacheManager cacheManager = CompilerCacheManager.getInstance(context.getProject());
        try {
            return cacheManager.getFileProcessingCompilerCache(myCompiler);
        }
        catch (IOException e) {
            cacheManager.clearCaches(context);
        }
        return cacheManager.getFileProcessingCompilerCache(myCompiler);
    }
}
