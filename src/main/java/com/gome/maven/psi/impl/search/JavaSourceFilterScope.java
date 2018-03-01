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

/*
 * @author max
 */
package com.gome.maven.psi.impl.search;

import com.gome.maven.ide.highlighter.JavaClassFileType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.JdkOrderEntry;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.SdkResolveScopeProvider;
import com.gome.maven.psi.search.DelegatingGlobalSearchScope;
import com.gome.maven.psi.search.GlobalSearchScope;

public class JavaSourceFilterScope extends DelegatingGlobalSearchScope {
    private static final Logger LOG = Logger.getInstance(JavaSourceFilterScope.class);

    
    private final ProjectFileIndex myIndex;

    public JavaSourceFilterScope( final GlobalSearchScope delegate) {
        super(delegate);

        Project project = getProject();
        if (project != null) {
            myIndex = ProjectRootManager.getInstance(project).getFileIndex();
        }
        else {
            myIndex = null;
            LOG.error("delegate.getProject() == null, delegate.getClass() == " + delegate.getClass());
        }
    }

    @Override
    public boolean contains( final VirtualFile file) {
        if (!super.contains(file)) {
            return false;
        }

        if (myIndex == null) {
            return false;
        }

        if (JavaClassFileType.INSTANCE == file.getFileType()) {
            return myIndex.isInLibraryClasses(file);
        }

        if (myIndex.isInSourceContent(file)) {
            return true;
        }
        final Project project = getProject();

        if (project != null) {
            for (OrderEntry entry : myIndex.getOrderEntriesForFile(file)) {
                if (entry instanceof JdkOrderEntry) {
                    final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)entry;

                    for (SdkResolveScopeProvider provider : SdkResolveScopeProvider.EP_NAME.getExtensions()) {
                        final GlobalSearchScope scope = provider.getScope(project, jdkOrderEntry);

                        if (scope != null && scope.contains(file)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}