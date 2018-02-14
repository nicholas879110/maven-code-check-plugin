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
package com.gome.maven.tools;

import com.gome.maven.openapi.actionSystem.ex.ActionManagerEx;
import com.gome.maven.openapi.components.ExportableApplicationComponent;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.options.SchemeProcessor;
import com.gome.maven.openapi.options.SchemesManager;
import com.gome.maven.openapi.options.SchemesManagerFactory;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.SmartList;
import gnu.trove.THashSet;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class BaseToolManager<T extends Tool> implements ExportableApplicationComponent {
     private final ActionManagerEx myActionManager;
    private final SchemesManager<ToolsGroup<T>, ToolsGroup<T>> mySchemesManager;

    public BaseToolManager( ActionManagerEx actionManagerEx, SchemesManagerFactory factory) {
        myActionManager = actionManagerEx;

        mySchemesManager = factory.createSchemesManager(getSchemesPath(), createProcessor(), RoamingType.PER_USER);
        mySchemesManager.loadSchemes();
        registerActions();
    }

    protected abstract String getSchemesPath();

    protected abstract SchemeProcessor<ToolsGroup<T>> createProcessor();

    
    public static String convertString(String s) {
        return StringUtil.nullize(s, true);
    }

    @Override
    
    public File[] getExportFiles() {
        return new File[]{mySchemesManager.getRootDirectory()};
    }

    @Override
    
    public String getPresentableName() {
        return ToolsBundle.message("tools.settings");
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    public void initComponent() {
    }

    public List<T> getTools() {
        List<T> result = new SmartList<T>();
        for (ToolsGroup<T> group : mySchemesManager.getAllSchemes()) {
            result.addAll(group.getElements());
        }
        return result;
    }

    
    public List<T> getTools( String group) {
        ToolsGroup<T> groupByName = mySchemesManager.findSchemeByName(group);
        if (groupByName == null) {
            return Collections.emptyList();
        }
        else {
            return groupByName.getElements();
        }
    }

    public String getGroupByActionId(String actionId) {
        for (T tool : getTools()) {
            if (Comparing.equal(actionId, tool.getActionId())) {
                return tool.getGroup();
            }
        }
        return null;
    }

    public List<ToolsGroup<T>> getGroups() {
        return mySchemesManager.getAllSchemes();
    }

    public void setTools(ToolsGroup[] tools) {
        mySchemesManager.clearAllSchemes();
        for (ToolsGroup newGroup : tools) {
            mySchemesManager.addNewScheme(newGroup, true);
        }
        registerActions();
    }

    void registerActions() {
        unregisterActions();

        // register
        // to prevent exception if 2 or more targets have the same name
        Set<String> registeredIds = new THashSet<String>();
        List<T> tools = getTools();
        for (T tool : tools) {
            String actionId = tool.getActionId();
            if (registeredIds.add(actionId)) {
                myActionManager.registerAction(actionId, createToolAction(tool));
            }
        }
    }

    
    protected ToolAction createToolAction( T tool) {
        return new ToolAction(tool);
    }

    protected abstract String getActionIdPrefix();

    private void unregisterActions() {
        // unregister Tool actions
        for (String oldId : myActionManager.getActionIds(getActionIdPrefix())) {
            myActionManager.unregisterAction(oldId);
        }
    }
}
