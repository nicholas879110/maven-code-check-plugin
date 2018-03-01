/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.localVcs.UpToDateLineNumberProvider;
import com.gome.maven.openapi.vcs.annotate.FileAnnotation;
import com.gome.maven.openapi.vcs.annotate.LineNumberListener;
import com.gome.maven.util.ui.TextTransferable;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;

/**
 * @author Konstantin Bulenkov
 */
public class CopyRevisionNumberFromAnnotateAction extends AnAction implements LineNumberListener {
    private final UpToDateLineNumberProvider myGetUpToDateLineNumber;
    private final FileAnnotation myAnnotation;
    private int myLineNumber = -1;

    public CopyRevisionNumberFromAnnotateAction(UpToDateLineNumberProvider getUpToDateLineNumber, FileAnnotation annotation) {
        super("Copy revision number");
        myGetUpToDateLineNumber = getUpToDateLineNumber;
        myAnnotation = annotation;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (myLineNumber < 0) return;
        final int corrected = myGetUpToDateLineNumber.getLineNumber(myLineNumber);
        final VcsRevisionNumber revisionNumber = myAnnotation.getLineRevisionNumber(corrected);
        if (revisionNumber != null) {
            final String revision = revisionNumber.asString();
            CopyPasteManager.getInstance().setContents(new TextTransferable(revision));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        int corrected = myLineNumber;
        final boolean enabled = myLineNumber >= 0 && (corrected = myGetUpToDateLineNumber.getLineNumber(myLineNumber)) >= 0 &&
                myAnnotation.getLineRevisionNumber(corrected) != null;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }

    @Override
    public void consume(Integer integer) {
        myLineNumber = integer;
    }
}
