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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.AsyncUpdateAction;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.util.containers.HashSet;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractVcsAction extends AsyncUpdateAction<VcsContext> implements DumbAware {
    public static Collection<AbstractVcs> getActiveVcses(VcsContext dataContext) {
        Collection<AbstractVcs> result = new HashSet<AbstractVcs>();
        final Project project = dataContext.getProject();
        if (project != null) {
            Collections.addAll(result, ProjectLevelVcsManager.getInstance(project).getAllActiveVcss());
        }
        return result;
    }

    
    protected static FilePath[] filterDescindingFiles( FilePath[] roots, Project project) {
        return DescindingFilesFilter.filterDescindingFiles(roots, project);
    }

    @Override
    protected VcsContext prepareDataFromContext(final AnActionEvent e) {
        return forceSyncUpdate(e) ? VcsContextWrapper.createInstanceOn(e) : VcsContextWrapper.createCachedInstanceOn(e);
    }

    @Override
    protected void performUpdate(final Presentation presentation, final VcsContext data) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                update(data, presentation);
            }
        });
    }

    @Override
    public final void actionPerformed( AnActionEvent e) {
        actionPerformed(VcsContextWrapper.createCachedInstanceOn(e));
    }

    protected abstract void actionPerformed( VcsContext e);

    protected abstract void update(VcsContext vcsContext, Presentation presentation);

}
