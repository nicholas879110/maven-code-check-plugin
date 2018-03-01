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
package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.changes.committed.CommittedChangesBrowserUseCase;
import com.gome.maven.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;

/**
 * @author yole
 */
public class OpenRepositoryVersionAction extends AnAction implements DumbAware {
    public OpenRepositoryVersionAction() {
        // TODO[yole]: real icon
        super(VcsBundle.message("open.repository.version.text"), VcsBundle.message("open.repository.version.description"),
                AllIcons.ObjectBrowser.ShowEditorHighlighting);
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        Change[] changes = e.getData(VcsDataKeys.SELECTED_CHANGES);
        assert changes != null;
        for(Change change: changes) {
            ContentRevision revision = change.getAfterRevision();
            if (revision == null || revision.getFile().isDirectory()) continue;
            VirtualFile vFile = ContentRevisionVirtualFile.create(revision);
            Navigatable navigatable = new OpenFileDescriptor(project, vFile);
            navigatable.navigate(true);
        }
    }

    public void update(final AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        Change[] changes = e.getData(VcsDataKeys.SELECTED_CHANGES);
        e.getPresentation().setEnabled(project != null && changes != null &&
                (! CommittedChangesBrowserUseCase.IN_AIR.equals(CommittedChangesBrowserUseCase.DATA_KEY.getData(e.getDataContext()))) &&
                hasValidChanges(changes) &&
                ModalityState.NON_MODAL.equals(ModalityState.current()));
    }

    private static boolean hasValidChanges(final Change[] changes) {
        for(Change c: changes) {
            final ContentRevision contentRevision = c.getAfterRevision();
            if (contentRevision != null && !contentRevision.getFile().isDirectory()) {
                return true;
            }
        }
        return false;
    }
}
