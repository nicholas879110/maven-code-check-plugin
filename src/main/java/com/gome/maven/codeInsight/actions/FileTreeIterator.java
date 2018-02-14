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
package com.gome.maven.codeInsight.actions;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.*;

public class FileTreeIterator {
    private Queue<PsiFile> myCurrentFiles = new LinkedList<PsiFile>();
    private Queue<PsiDirectory> myCurrentDirectories = new LinkedList<PsiDirectory>();

    public FileTreeIterator( List<PsiFile> files) {
        myCurrentFiles.addAll(files);
    }

    public FileTreeIterator( Module module) {
        myCurrentDirectories.addAll(collectModuleDirectories(module));
        expandDirectoriesUntilFilesNotEmpty();
    }

    public FileTreeIterator( Project project) {
        myCurrentDirectories.addAll(collectProjectDirectories(project));
        expandDirectoriesUntilFilesNotEmpty();
    }

    
    public static List<PsiDirectory> collectProjectDirectories( Project project) {
        List<PsiDirectory> directories = ContainerUtil.newArrayList();

        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            directories.addAll(collectModuleDirectories(module));
        }

        return directories;
    }

    public FileTreeIterator( PsiDirectory directory) {
        myCurrentDirectories.add(directory);
        expandDirectoriesUntilFilesNotEmpty();
    }

    public FileTreeIterator( FileTreeIterator fileTreeIterator) {
        myCurrentFiles = new LinkedList<PsiFile>(fileTreeIterator.myCurrentFiles);
        myCurrentDirectories = new LinkedList<PsiDirectory>(fileTreeIterator.myCurrentDirectories);
    }

    
    public PsiFile next() {
        if (myCurrentFiles.isEmpty()) {
            throw new NoSuchElementException();
        }
        PsiFile current = myCurrentFiles.poll();
        expandDirectoriesUntilFilesNotEmpty();
        return current;
    }

    public boolean hasNext() {
        return !myCurrentFiles.isEmpty();
    }

    private void expandDirectoriesUntilFilesNotEmpty() {
        while (myCurrentFiles.isEmpty() && !myCurrentDirectories.isEmpty()) {
            PsiDirectory dir = myCurrentDirectories.poll();
            expandDirectory(dir);
        }
    }

    private void expandDirectory( PsiDirectory dir) {
        Collections.addAll(myCurrentFiles, dir.getFiles());
        Collections.addAll(myCurrentDirectories, dir.getSubdirectories());
    }

    
    public static List<PsiDirectory> collectModuleDirectories(Module module) {
        List<PsiDirectory> dirs = ContainerUtil.newArrayList();

        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        for (VirtualFile root : contentRoots) {
            PsiDirectory dir = PsiManager.getInstance(module.getProject()).findDirectory(root);
            if (dir != null) {
                dirs.add(dir);
            }
        }

        return dirs;
    }
}
