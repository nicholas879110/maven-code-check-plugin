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
package com.gome.maven.diff.actions.impl;

import com.gome.maven.diff.contents.*;
import com.gome.maven.diff.requests.ContentDiffRequest;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.tools.util.DiffDataKeys;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diff.DiffManager;
import com.gome.maven.openapi.diff.SimpleContent;
import com.gome.maven.openapi.diff.SimpleDiffRequest;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.List;

public class ShowOldDiffAction extends DumbAwareAction {
     public static final Object DO_NOT_TRY_MIGRATE = "doNotTryMigrate";

    public ShowOldDiffAction() {
        super("Show in Old diff", null, AllIcons.Diff.Diff);
    }

    @Override
    public void update(AnActionEvent e) {
        if (!ApplicationManager.getApplication().isInternal()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);

        DiffRequest request = e.getData(DiffDataKeys.DIFF_REQUEST);
        if (request == null || !(request instanceof ContentDiffRequest)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
        if (contents.size() != 2) {
            e.getPresentation().setEnabled(false);
            return;
        }

        for (DiffContent content : contents) {
            if (content instanceof EmptyContent ||
                    content instanceof DocumentContent ||
                    content instanceof FileContent ||
                    content instanceof DirectoryContent) {
                continue;
            }

            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ContentDiffRequest request = (ContentDiffRequest)e.getRequiredData(DiffDataKeys.DIFF_REQUEST);
        List<DiffContent> contents = request.getContents();
        List<String> titles = request.getContentTitles();

        SimpleDiffRequest newRequest = new SimpleDiffRequest(e.getProject(), request.getTitle());
        newRequest.setContentTitles(titles.get(0), titles.get(1));
        newRequest.setContents(convert(e.getProject(), contents.get(0)), convert(e.getProject(), contents.get(1)));
        newRequest.addHint(DO_NOT_TRY_MIGRATE);

        DiffManager.getInstance().getDiffTool().show(newRequest);
    }

    
    private static com.gome.maven.openapi.diff.DiffContent convert( Project project,  DiffContent content) {
        if (content instanceof EmptyContent) return SimpleContent.createEmpty();

        if (content instanceof DocumentContent) {
            Document document = ((DocumentContent)content).getDocument();
            return new com.gome.maven.openapi.diff.DocumentContent(project, document, content.getContentType());
        }
        if (content instanceof FileContent) {
            VirtualFile file = ((FileContent)content).getFile();
            return new com.gome.maven.openapi.diff.FileContent(project, file);
        }
        if (content instanceof DirectoryContent) {
            VirtualFile file = ((DirectoryContent)content).getFile();
            return new com.gome.maven.openapi.diff.FileContent(project, file);
        }

        throw new IllegalArgumentException();
    }
}
