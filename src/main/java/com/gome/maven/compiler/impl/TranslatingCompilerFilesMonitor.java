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
package com.gome.maven.compiler.impl;

import com.gome.maven.compiler.server.BuildManager;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ProjectUtil;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFile;
import com.gome.maven.util.Function;
import gnu.trove.THashSet;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * @since Jun 3, 2008
 *
 * A source file is scheduled for recompilation if
 * 1. its timestamp has changed
 * 2. one of its corresponding output files was deleted
 * 3. output root of containing module has changed
 *
 * An output file is scheduled for deletion if:
 * 1. corresponding source file has been scheduled for recompilation (see above)
 * 2. corresponding source file has been deleted
 */
public class TranslatingCompilerFilesMonitor implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.TranslatingCompilerFilesMonitor");
    public static boolean ourDebugMode = false;

    public TranslatingCompilerFilesMonitor(VirtualFileManager vfsManager, Application application) {
        vfsManager.addVirtualFileListener(new MyVfsListener(), application);
    }

    public static TranslatingCompilerFilesMonitor getInstance() {
        return ApplicationManager.getApplication().getComponent(TranslatingCompilerFilesMonitor.class);
    }

    
    public String getComponentName() {
        return "TranslatingCompilerFilesMonitor";
    }

    public void initComponent() {
    }


    public void disposeComponent() {
    }

    private interface FileProcessor {
        void execute(VirtualFile file);
    }

    private static void processRecursively(final VirtualFile fromFile, final boolean dbOnly, final FileProcessor processor) {
        if (!(fromFile.getFileSystem() instanceof LocalFileSystem)) {
            return;
        }

        VfsUtilCore.visitChildrenRecursively(fromFile, new VirtualFileVisitor() {
             @Override
            public Result visitFileEx( VirtualFile file) {
                if (isIgnoredByBuild(file)) {
                    return SKIP_CHILDREN;
                }

                if (!file.isDirectory()) {
                    processor.execute(file);
                }
                return CONTINUE;
            }

            
            @Override
            public Iterable<VirtualFile> getChildrenIterable( VirtualFile file) {
                if (dbOnly) {
                    return file.isDirectory()? ((NewVirtualFile)file).iterInDbChildren() : null;
                }
                if (file.equals(fromFile) || !file.isDirectory()) {
                    return null; // skipping additional checks for the initial file and non-directory files
                }
                // optimization: for all files that are not under content of currently opened projects iterate over DB children
                return isInContentOfOpenedProject(file)? null : ((NewVirtualFile)file).iterInDbChildren();
            }
        });
    }

    private static boolean isInContentOfOpenedProject( final VirtualFile file) {
        // probably need a read action to ensure that the project was not disposed during the iteration over the project list
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isInitialized() || !BuildManager.getInstance().isProjectWatched(project)) {
                continue;
            }
            if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
                return true;
            }
        }
        return false;
    }

    private static class MyVfsListener extends VirtualFileAdapter {
        public void propertyChanged( final VirtualFilePropertyEvent event) {
            if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
                final VirtualFile eventFile = event.getFile();
                if (isInContentOfOpenedProject(eventFile)) {
                    final VirtualFile parent = event.getParent();
                    if (parent != null) {
                        final String oldName = (String)event.getOldValue();
                        final String root = parent.getPath() + "/" + oldName;
                        final Set<File> toMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
                        if (eventFile.isDirectory()) {
                            VfsUtilCore.visitChildrenRecursively(eventFile, new VirtualFileVisitor() {
                                private StringBuilder filePath = new StringBuilder(root);

                                @Override
                                public boolean visitFile( VirtualFile child) {
                                    if (child.isDirectory()) {
                                        if (!Comparing.equal(child, eventFile)) {
                                            filePath.append("/").append(child.getName());
                                        }
                                    }
                                    else {
                                        String childPath = filePath.toString();
                                        if (!Comparing.equal(child, eventFile)) {
                                            childPath += "/" + child.getName();
                                        }
                                        toMark.add(new File(childPath));
                                    }
                                    return true;
                                }

                                @Override
                                public void afterChildrenVisited( VirtualFile file) {
                                    if (file.isDirectory() && !Comparing.equal(file, eventFile)) {
                                        filePath.delete(filePath.length() - file.getName().length() - 1, filePath.length());
                                    }
                                }
                            });
                        }
                        else {
                            toMark.add(new File(root));
                        }
                        notifyFilesDeleted(toMark);
                    }
                    collectPathsAndNotify(eventFile, NOTIFY_CHANGED);
                }
            }
        }

        public void contentsChanged( final VirtualFileEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_CHANGED);
        }

        public void fileCreated( final VirtualFileEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_CHANGED);
        }

        public void fileCopied( final VirtualFileCopyEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_CHANGED);
        }

        public void fileMoved( VirtualFileMoveEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_CHANGED);
        }

        public void beforeFileDeletion( final VirtualFileEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_DELETED);
        }

        public void beforeFileMovement( final VirtualFileMoveEvent event) {
            collectPathsAndNotify(event.getFile(), NOTIFY_DELETED);
        }
    }


    private static final Function<Collection<File>, Void> NOTIFY_CHANGED = new Function<Collection<File>, Void>() {
        public Void fun(Collection<File> files) {
            notifyFilesChanged(files);
            return null;
        }
    };

    private static final Function<Collection<File>, Void> NOTIFY_DELETED = new Function<Collection<File>, Void>() {
        public Void fun(Collection<File> files) {
            notifyFilesDeleted(files);
            return null;
        }
    };

    private static void collectPathsAndNotify(final VirtualFile file, final Function<Collection<File>, Void> notification) {
        final Set<File> pathsToMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
        if (!isIgnoredOrUnderIgnoredDirectory(file)) {
            final boolean inContent = isInContentOfOpenedProject(file);
            processRecursively(file, !inContent, new FileProcessor() {
                public void execute(final VirtualFile file) {
                    pathsToMark.add(new File(file.getPath()));
                }
            });
        }
        if (!pathsToMark.isEmpty()) {
            notification.fun(pathsToMark);
        }
    }

    private static boolean isIgnoredOrUnderIgnoredDirectory(final VirtualFile file) {
        if (isIgnoredByBuild(file)) {
            return true;
        }
        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        VirtualFile current = file.getParent();
        while (current != null) {
            if (fileTypeManager.isFileIgnored(current)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isIgnoredByBuild(VirtualFile file) {
        return
                FileTypeManager.getInstance().isFileIgnored(file) ||
                        ProjectUtil.isProjectOrWorkspaceFile(file)        ||
                        FileUtil.isAncestor(PathManager.getConfigPath(), file.getPath(), false); // is config file
    }

    private static void notifyFilesChanged(Collection<File> paths) {
        if (!paths.isEmpty()) {
            BuildManager.getInstance().notifyFilesChanged(paths);
        }
    }

    private static void notifyFilesDeleted(Collection<File> paths) {
        if (!paths.isEmpty()) {
            BuildManager.getInstance().notifyFilesDeleted(paths);
        }
    }

}
