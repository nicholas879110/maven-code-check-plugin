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
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeListManager;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.changes.CurrentContentRevision;
import com.gome.maven.openapi.vcs.changes.committed.CommittedChangesBrowserUseCase;

import java.util.ArrayList;
import java.util.List;

import static com.gome.maven.openapi.vcs.changes.actions.diff.ShowDiffAction.*;

/**
 * @author yole
 */
public class ShowDiffWithLocalAction extends AnAction implements DumbAware {
    public ShowDiffWithLocalAction() {
        super(VcsBundle.message("show.diff.with.local.action.text"),
                VcsBundle.message("show.diff.with.local.action.description"),
                AllIcons.Actions.DiffWithCurrent);
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        assert changes != null;
        List<Change> changesToLocal = new ArrayList<Change>();
        for(Change change: changes) {
            ContentRevision afterRevision = change.getAfterRevision();
            if (afterRevision != null && isValidAfterRevision(afterRevision)) {
                changesToLocal.add(new Change(afterRevision, CurrentContentRevision.create(afterRevision.getFile())));
            }
        }
        if (!changesToLocal.isEmpty()) {
            showDiffForChange(project, changesToLocal, 0);
        }
    }

    public void update(final AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        Change[] changes = e.getData(VcsDataKeys.CHANGES);

        e.getPresentation().setEnabled(project != null && changes != null &&
                (! CommittedChangesBrowserUseCase.IN_AIR
                        .equals(CommittedChangesBrowserUseCase.DATA_KEY.getData(e.getDataContext()))) &&
                anyHasAfterRevision(changes));
    }

    private static boolean isValidAfterRevision(final ContentRevision afterRevision) {
        return afterRevision != null && !afterRevision.getFile().isNonLocal() && !afterRevision.getFile().isDirectory();
    }

    private static boolean anyHasAfterRevision(final Change[] changes) {
        for(Change c: changes) {
            if (isValidAfterRevision(c.getAfterRevision())) {
                return true;
            }
        }
        return false;
    }
}
