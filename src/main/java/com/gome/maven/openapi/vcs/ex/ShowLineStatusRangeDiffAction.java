/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.ex;

import com.gome.maven.diff.DiffContentFactory;
import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffManager;
import com.gome.maven.diff.actions.DocumentFragmentContent;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.contents.DocumentContent;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vfs.VirtualFile;

public class ShowLineStatusRangeDiffAction extends BaseLineStatusRangeAction {
    public ShowLineStatusRangeDiffAction( LineStatusTracker lineStatusTracker,  Range range,  Editor editor) {
        super(VcsBundle.message("action.name.show.difference"), AllIcons.Actions.Diff, lineStatusTracker, range);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        DiffManager.getInstance().showDiff(e.getProject(), createDiffData());
    }

    private DiffRequest createDiffData() {
        Range range = expand(myRange, myLineStatusTracker.getDocument(), myLineStatusTracker.getVcsDocument());

        DiffContent vcsContent = createDiffContent(myLineStatusTracker.getVcsDocument(),
                myLineStatusTracker.getVcsRange(range),
                null);
        DiffContent currentContent = createDiffContent(myLineStatusTracker.getDocument(),
                myLineStatusTracker.getCurrentTextRange(range),
                myLineStatusTracker.getVirtualFile());

        return new SimpleDiffRequest(VcsBundle.message("dialog.title.diff.for.range"),
                vcsContent, currentContent,
                VcsBundle.message("diff.content.title.up.to.date"),
                VcsBundle.message("diff.content.title.current.range")
        );
    }

    
    private DiffContent createDiffContent( Document document,  TextRange textRange,  VirtualFile file) {
        final Project project = myLineStatusTracker.getProject();
        DocumentContent content = DiffContentFactory.getInstance().create(project, document, file);
        return new DocumentFragmentContent(project, content, textRange);
    }

    
    private static Range expand( Range range,  Document document,  Document uDocument) {
        boolean canExpandBefore = range.getLine1() != 0 && range.getVcsLine1() != 0;
        boolean canExpandAfter = range.getLine2() < document.getLineCount() && range.getVcsLine2() < uDocument.getLineCount();
        int offset1 = range.getLine1() - (canExpandBefore ? 1 : 0);
        int uOffset1 = range.getVcsLine1() - (canExpandBefore ? 1 : 0);
        int offset2 = range.getLine2() + (canExpandAfter ? 1 : 0);
        int uOffset2 = range.getVcsLine2() + (canExpandAfter ? 1 : 0);
        return new Range(offset1, offset2, uOffset1, uOffset2, range.getType());
    }
}