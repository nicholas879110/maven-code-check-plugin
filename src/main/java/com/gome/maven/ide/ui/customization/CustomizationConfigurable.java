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
package com.gome.maven.ide.ui.customization;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.options.BaseConfigurable;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SearchableConfigurable;

import javax.swing.*;

/**
 * User: anna
 * Date: Mar 17, 2005
 */
public class CustomizationConfigurable extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private CustomizableActionsPanel myPanel;

    public JComponent createComponent() {
        if (myPanel == null) {
            myPanel = new CustomizableActionsPanel();
        }
        return myPanel.getPanel();
    }

    public String getDisplayName() {
        return IdeBundle.message("title.customizations");
    }

    public String getHelpTopic() {
        return "preferences.customizations";
    }

    public void apply() throws ConfigurationException {
        myPanel.apply();
    }

    public void reset() {
        myPanel.reset();
    }

    public boolean isModified() {
        return myPanel.isModified();
    }

    public void disposeUIResources() {
    }

    public String getId() {
        return getHelpTopic();
    }

    public Runnable enableSearch(String option) {
        return null;
    }
}
