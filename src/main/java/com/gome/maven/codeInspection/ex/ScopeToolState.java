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

/*
 * User: anna
 * Date: 20-Apr-2009
 */
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;
import org.jdom.Element;

import javax.swing.*;

public class ScopeToolState {
    private NamedScope myScope;
    
    private final String myScopeName;
    private InspectionToolWrapper myToolWrapper;
    private boolean myEnabled;
    private HighlightDisplayLevel myLevel;

    private boolean myAdditionalConfigPanelCreated = false;
    private JComponent myAdditionalConfigPanel;
    private static final Logger LOG = Logger.getInstance("#" + ScopeToolState.class.getName());

    public ScopeToolState( NamedScope scope,  InspectionToolWrapper toolWrapper, boolean enabled,  HighlightDisplayLevel level) {
        this(scope.getName(), toolWrapper, enabled, level);
        myScope = scope;
    }

    public ScopeToolState( String scopeName,  InspectionToolWrapper toolWrapper, boolean enabled,  HighlightDisplayLevel level) {
        myScopeName = scopeName;
        myToolWrapper = toolWrapper;
        myEnabled = enabled;
        myLevel = level;
    }

    public ScopeToolState copy() {
        return new ScopeToolState(myScopeName, myToolWrapper, myEnabled, myLevel);
    }

    
    public NamedScope getScope(Project project) {
        if (myScope == null && project != null) {
            myScope = NamedScopesHolder.getScope(project, myScopeName);
        }
        return myScope;
    }

    
    public String getScopeName() {
        return myScopeName;
    }

    
    public InspectionToolWrapper getTool() {
        return myToolWrapper;
    }

    public boolean isEnabled() {
        return myEnabled;
    }

    
    public HighlightDisplayLevel getLevel() {
        return myLevel;
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    public void setLevel( HighlightDisplayLevel level) {
        myLevel = level;
    }

    
    public JComponent getAdditionalConfigPanel() {
        if (!myAdditionalConfigPanelCreated) {
            myAdditionalConfigPanel = myToolWrapper.getTool().createOptionsPanel();
            myAdditionalConfigPanelCreated = true;
        }
        return myAdditionalConfigPanel;
    }

    public void resetConfigPanel(){
        myAdditionalConfigPanelCreated = false;
        myAdditionalConfigPanel = null;
    }

    public void setTool( InspectionToolWrapper tool) {
        myToolWrapper = tool;
    }

    public boolean equalTo( ScopeToolState state2) {
        if (isEnabled() != state2.isEnabled()) return false;
        if (getLevel() != state2.getLevel()) return false;
        InspectionToolWrapper toolWrapper = getTool();
        InspectionToolWrapper toolWrapper2 = state2.getTool();
        if (!toolWrapper.isInitialized() && !toolWrapper2.isInitialized()) return true;
        try {
             String tempRoot = "root";
            Element oldToolSettings = new Element(tempRoot);
            toolWrapper.getTool().writeSettings(oldToolSettings);
            Element newToolSettings = new Element(tempRoot);
            toolWrapper2.getTool().writeSettings(newToolSettings);
            return JDOMUtil.areElementsEqual(oldToolSettings, newToolSettings);
        }
        catch (WriteExternalException e) {
            LOG.error(e);
        }
        return false;
    }

    public void scopesChanged() {
        myScope = null;
    }
}