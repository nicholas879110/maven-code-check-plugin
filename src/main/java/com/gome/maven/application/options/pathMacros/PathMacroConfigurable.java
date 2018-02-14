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
package com.gome.maven.application.options.pathMacros;

import com.gome.maven.openapi.application.ApplicationBundle;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SearchableConfigurable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ex.ProjectEx;

import javax.swing.*;

/**
 * @author dsl
 */
public class PathMacroConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    
    public static final String HELP_ID = "preferences.pathVariables";
    private PathMacroListEditor myEditor;

    @Override
    public JComponent createComponent() {
        myEditor = new PathMacroListEditor();
        return myEditor.getPanel();
    }

    @Override
    public void apply() throws ConfigurationException {
        myEditor.commit();

        final Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            ((ProjectEx)project).checkUnknownMacros(false);
        }
    }

    @Override
    public void reset() {
        myEditor.reset();
    }

    @Override
    public void disposeUIResources() {
        myEditor = null;
    }

    @Override
    public String getDisplayName() {
        return ApplicationBundle.message("title.path.variables");
    }

    @Override
    
    public String getHelpTopic() {
        return HELP_ID;
    }

    @Override
    public boolean isModified() {
        return myEditor != null && myEditor.isModified();
    }

    @Override
    
    public String getId() {
        return getHelpTopic();
    }

    @Override
    
    public Runnable enableSearch(String option) {
        return null;
    }
}
