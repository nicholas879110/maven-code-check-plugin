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
package com.gome.maven.openapi.project.impl;

import com.gome.maven.application.options.pathMacros.PathMacroConfigurable;
import com.gome.maven.application.options.pathMacros.PathMacroListEditor;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.ui.MultiLineLabelUI;
import com.gome.maven.ui.IdeBorderFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 4, 2004
 */
public class UndefinedMacrosConfigurable implements Configurable{
    private PathMacroListEditor myEditor;
    private final String myText;
    private final Collection<String> myUndefinedMacroNames;

    public UndefinedMacrosConfigurable(String text, Collection<String> undefinedMacroNames) {
        myText = text;
        myUndefinedMacroNames = undefinedMacroNames;
    }

    public String getHelpTopic() {
        return PathMacroConfigurable.HELP_ID;
    }

    public String getDisplayName() {
        return ProjectBundle.message("project.configure.path.variables.title");
    }

    public JComponent createComponent() {
        final JPanel mainPanel = new JPanel(new BorderLayout());
        // important: do not allow to remove or change macro name for already defined macros befor project is loaded
        myEditor = new PathMacroListEditor(myUndefinedMacroNames);
        final JComponent editorPanel = myEditor.getPanel();

        mainPanel.add(editorPanel, BorderLayout.CENTER);

        final JLabel textLabel = new JLabel(myText);
        textLabel.setUI(new MultiLineLabelUI());
        textLabel.setBorder(IdeBorderFactory.createEmptyBorder(6, 6, 6, 6));
        mainPanel.add(textLabel, BorderLayout.NORTH);

        return mainPanel;
    }

    public boolean isModified() {
        return myEditor.isModified();
    }

    public void apply() throws ConfigurationException {
        myEditor.commit();
    }

    public void reset() {
        myEditor.reset();
    }

    public void disposeUIResources() {
        myEditor = null;
    }
}
