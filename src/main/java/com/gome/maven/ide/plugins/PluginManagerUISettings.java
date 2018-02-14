/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.ide.plugins;

import com.gome.maven.ide.ui.SplitterProportionsDataImpl;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.progress.PerformInBackgroundOption;
import com.gome.maven.util.xmlb.XmlSerializerUtil;
import com.gome.maven.util.xmlb.annotations.Attribute;

import javax.swing.*;

@State(
        name = "PluginManagerConfigurable",
        storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/plugin_ui.xml")
)
public class PluginManagerUISettings implements PersistentStateComponent<PluginManagerUISettings>, PerformInBackgroundOption {
    public int AVAILABLE_SORT_COLUMN_ORDER = SortOrder.ASCENDING.ordinal();

    public boolean availableSortByStatus;
    public boolean installedSortByStatus;

    public boolean UPDATE_IN_BACKGROUND;

    @Attribute(converter = SplitterProportionsDataImpl.SplitterProportionsConverter.class)
    public SplitterProportionsDataImpl installedProportions = new SplitterProportionsDataImpl();
    @Attribute(converter = SplitterProportionsDataImpl.SplitterProportionsConverter.class)
    public SplitterProportionsDataImpl availableProportions = new SplitterProportionsDataImpl();

    public PluginManagerUISettings() {
        Float defaultProportion = new Float(0.5);
        installedProportions.getProportions().add(defaultProportion);
        availableProportions.getProportions().add(defaultProportion);
    }

    public static PluginManagerUISettings getInstance() {
        return ServiceManager.getService(PluginManagerUISettings.class);
    }

    @Override
    public PluginManagerUISettings getState() {
        return this;
    }

    @Override
    public void loadState(PluginManagerUISettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public boolean shouldStartInBackground() {
        return UPDATE_IN_BACKGROUND;
    }

    @Override
    public void processSentToBackground() {
        UPDATE_IN_BACKGROUND = true;
    }
}
