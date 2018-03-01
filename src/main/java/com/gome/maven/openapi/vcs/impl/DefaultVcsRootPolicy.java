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
package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.vcs.changes.DirtBuilder;
import com.gome.maven.openapi.vcs.changes.VcsGuess;
import com.gome.maven.openapi.vcs.impl.projectlevelman.NewMappings;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * @author yole
 */
public abstract class DefaultVcsRootPolicy {
    public static DefaultVcsRootPolicy getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetService(project, DefaultVcsRootPolicy.class);
    }

    public abstract void addDefaultVcsRoots(final NewMappings mappingList,  String vcsName, List<VirtualFile> result);

    public abstract boolean matchesDefaultMapping(final VirtualFile file, final Object matchContext);

    
    public abstract Object getMatchContext(final VirtualFile file);

    
    public abstract VirtualFile getVcsRootFor(final VirtualFile file);

    public abstract void markDefaultRootsDirty(final DirtBuilder builder, VcsGuess vcsGuess);

    public String getProjectConfigurationMessage(final Project project) {
        final StorageScheme storageScheme = ((ProjectEx) project).getStateStore().getStorageScheme();
        boolean isDirectoryBased = StorageScheme.DIRECTORY_BASED.equals(storageScheme);
        final String[] parts = new String[] {"Content roots of all modules", "all immediate descendants of project base directory",
                Project.DIRECTORY_STORE_FOLDER + " directory contents"};
        final StringBuilder sb = new StringBuilder(parts[0]);
        if (isDirectoryBased) {
            sb.append(", ");
        } else {
            sb.append(", and ");
        }
        sb.append(parts[1]);
        if (isDirectoryBased) {
            sb.append(", and ");
            sb.append(parts[2]);
        }
        return sb.toString();
    }
}
