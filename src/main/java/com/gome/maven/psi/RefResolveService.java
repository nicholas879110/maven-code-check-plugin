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
package com.gome.maven.psi;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.AbstractProjectComponent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileWithId;
import com.gome.maven.psi.search.GlobalSearchScope;

import java.util.Collection;

public abstract class RefResolveService extends AbstractProjectComponent {
    /**
     * if true then PsiElement.getUseScope() returns scope restricted to only relevant files which are stored in {@link com.gome.maven.psi.RefResolveService}
     */
    public static final boolean ENABLED = /*ApplicationManager.getApplication().isUnitTestMode() ||*/ Boolean.getBoolean("ref.back");

    public RefResolveService(Project project) {
        super(project);
    }

    public static RefResolveService getInstance(Project project) {
        return project.getComponent(RefResolveService.class);
    }

    public abstract int[] getBackwardIds( VirtualFileWithId file);

    /**
     * @return subset of scope containing only files which reference the virtualFile
     */
    
    public abstract GlobalSearchScope restrictByBackwardIds( VirtualFile virtualFile,  GlobalSearchScope scope);

    /**
     * @return add files to the resolve queue. until all files from there are resolved, the service is in incomplete state and returns null from getBackwardIds()
     */
    public abstract boolean queue( Collection<VirtualFile> files,  Object reason);

    public abstract boolean isUpToDate();

    public abstract int getQueueSize();
    public abstract void addListener( Disposable parent,  Listener listener);
    public abstract static class Listener {
        public void fileResolved( VirtualFile virtualFile) {}
        public void allFilesResolved() {}
    }
}
