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
package com.gome.maven.openapi.vfs;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;

import java.util.Collection;

public abstract class ReadonlyStatusHandler {
    public static boolean ensureFilesWritable( Project project,  VirtualFile... files) {
        return !getInstance(project).ensureFilesWritable(files).hasReadonlyFiles();
    }

    public static boolean ensureDocumentWritable( Project project,  Document document) {
        final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        boolean okWritable;
        if (psiFile == null) {
            okWritable = document.isWritable();
        }
        else {
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null) {
                okWritable = ensureFilesWritable(project, virtualFile);
            }
            else {
                okWritable = psiFile.isWritable();
            }
        }
        return okWritable;
    }

    public abstract static class OperationStatus {
        
        public abstract VirtualFile[] getReadonlyFiles();

        public abstract boolean hasReadonlyFiles();

        
        public abstract String getReadonlyFilesMessage();
    }

    public abstract OperationStatus ensureFilesWritable( VirtualFile... files);

    public OperationStatus ensureFilesWritable( Collection<VirtualFile> files) {
        return ensureFilesWritable(VfsUtilCore.toVirtualFileArray(files));
    }

    public static ReadonlyStatusHandler getInstance(Project project) {
        return ServiceManager.getService(project, ReadonlyStatusHandler.class);
    }

}
