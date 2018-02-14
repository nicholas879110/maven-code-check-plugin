/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public abstract class IndexableSetContributor implements IndexedRootsProvider {

    protected static final Set<VirtualFile> EMPTY_FILE_SET = Collections.unmodifiableSet(new HashSet<VirtualFile>());

    @Override
    public final Set<String> getRootsToIndex() {
        return ContainerUtil.map2Set(getAdditionalRootsToIndex(), new NotNullFunction<VirtualFile, String>() {
            
            @Override
            public String fun(VirtualFile virtualFile) {
                return virtualFile.getUrl();
            }
        });
    }

    
    public static Set<VirtualFile> getProjectRootsToIndex(IndexedRootsProvider provider, Project project) {
        if (provider instanceof IndexableSetContributor) {
            return ((IndexableSetContributor)provider).getAdditionalProjectRootsToIndex(project);
        }
        return EMPTY_FILE_SET;
    }

    public static Set<VirtualFile> getRootsToIndex(IndexedRootsProvider provider) {
        if (provider instanceof IndexableSetContributor) {
            return ((IndexableSetContributor)provider).getAdditionalRootsToIndex();
        }

        final HashSet<VirtualFile> result = new HashSet<VirtualFile>();
        for (String url : provider.getRootsToIndex()) {
            ContainerUtil.addIfNotNull(VirtualFileManager.getInstance().findFileByUrl(url), result);
        }

        return result;
    }

    
    public Set<VirtualFile> getAdditionalProjectRootsToIndex( Project project) {
        return EMPTY_FILE_SET;
    }

    public abstract Set<VirtualFile> getAdditionalRootsToIndex();
}
