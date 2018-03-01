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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.vcs.changes.*;
import com.gome.maven.util.NullableConsumer;

import javax.swing.*;
import java.util.Collection;

/**
 * @author max
 */
public class ChangeListChooser extends DialogWrapper {
    private final Project myProject;
    private LocalChangeList mySelectedList;
    private final ChangeListChooserPanel myPanel;

    public ChangeListChooser( Project project,
                              Collection<? extends ChangeList> changelists,
                              ChangeList defaultSelection,
                             final String title,
                              final String defaultName) {
        super(project, false);
        myProject = project;

        ChangeListEditHandler handler;
        for (ChangeList changelist : changelists) {
            handler = ((LocalChangeListImpl)changelist).getEditHandler();
            if (handler != null) {
                break;
            }
        }

        myPanel = new ChangeListChooserPanel(myProject, new NullableConsumer<String>() {
            public void consume(final  String errorMessage) {
                setOKActionEnabled(errorMessage == null);
                setErrorText(errorMessage);
            }
        });

        myPanel.init();
        myPanel.setChangeLists(changelists);
        myPanel.setDefaultSelection(changelists.size() <= 1 && onlyOneListInProject() ? null : defaultSelection);

        setTitle(title);
        if (defaultName != null) {
            myPanel.setDefaultName(defaultName);
        }

        init();
    }

    private boolean onlyOneListInProject() {
        return ChangeListManager.getInstance(myProject).getChangeListsNumber() <= 1;
    }

    public JComponent getPreferredFocusedComponent() {
        return myPanel.getPreferredFocusedComponent();
    }

    protected String getDimensionServiceKey() {
        return "VCS.ChangelistChooser";
    }

    protected void doOKAction() {
        mySelectedList = myPanel.getSelectedList(myProject);
        if (mySelectedList != null) {
            super.doOKAction();
        }
    }

    public LocalChangeList getSelectedList() {
        return mySelectedList;
    }

    protected JComponent createCenterPanel() {
        return myPanel;
    }
}
