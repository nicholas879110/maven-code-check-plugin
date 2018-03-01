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
package com.gome.maven.ui;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.ComponentWithBrowseButton;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.util.ArrayUtil;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author ven
 */
public class ReferenceEditorComboWithBrowseButton extends ComponentWithBrowseButton<EditorComboBox> implements TextAccessor {
    public ReferenceEditorComboWithBrowseButton(final ActionListener browseActionListener,
                                                final String text,
                                                 final Project project,
                                                boolean toAcceptClasses, final String recentsKey) {
        this(browseActionListener, text, project, toAcceptClasses, JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE, recentsKey);
    }

    public ReferenceEditorComboWithBrowseButton(final ActionListener browseActionListener,
                                                final String text,
                                                 final Project project,
                                                boolean toAcceptClasses,
                                                final JavaCodeFragment.VisibilityChecker visibilityChecker, final String recentsKey) {
        super(new EditorComboBox(JavaReferenceEditorUtil.createDocument(StringUtil.isEmpty(text) ? "" : text, project, toAcceptClasses, visibilityChecker), project, StdFileTypes.JAVA),
                browseActionListener);
        final List<String> recentEntries = RecentsManager.getInstance(project).getRecentEntries(recentsKey);
        if (recentEntries != null) {
            setHistory(ArrayUtil.toStringArray(recentEntries));
        }
        if (text != null && text.length() > 0) {
            prependItem(text);
        }
    }

    public String getText(){
        return getChildComponent().getText().trim();
    }

    public void setText(final String text){
        getChildComponent().setText(text);
    }

    public boolean isEditable() {
        return !getChildComponent().getEditorEx().isViewer();
    }

    public void setHistory(String[] history) {
        getChildComponent().setHistory(history);
    }

    public void prependItem(String item) {
        getChildComponent().prependItem(item);
    }

    public void appendItem(String item) {
        getChildComponent().appendItem(item);
    }
}
