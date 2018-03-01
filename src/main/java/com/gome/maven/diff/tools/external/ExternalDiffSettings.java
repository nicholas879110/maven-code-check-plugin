/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.tools.external;

import com.gome.maven.diff.util.DiffUtil;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.diff.impl.external.DiffManagerImpl;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.config.AbstractProperty;
import com.gome.maven.util.config.BooleanProperty;
import com.gome.maven.util.config.StringProperty;

@State(
        name = "ExternalDiffSettings",
        storages = @Storage(file = DiffUtil.DIFF_CONFIG)
)
public class ExternalDiffSettings implements PersistentStateComponent<ExternalDiffSettings.State> {
    private State myState = new State();

    public State getState() {
        return myState;
    }

    public void loadState(State state) {
        myState = state;
    }

    public static ExternalDiffSettings getInstance() {
        return ServiceManager.getService(ExternalDiffSettings.class);
    }

    //
    // Migration from the old settings container. To be removed.
    //

    
    private static AbstractProperty.AbstractPropertyContainer getProperties() {
        return DiffManagerImpl.getInstanceEx().getProperties();
    }

    
    private static String getProperty( StringProperty oldProperty,  String newValue,  String defaultValue) {
        if (newValue != null) return newValue;
        if (oldProperty != null) {
            String oldValue = oldProperty.get(getProperties());
            if (!StringUtil.isEmptyOrSpaces(oldValue)) return oldValue;
        }
        return defaultValue;
    }

    private static boolean getProperty( BooleanProperty oldProperty,  Boolean newValue, boolean defaultValue) {
        if (newValue != null) return newValue;
        if (oldProperty != null) {
            return oldProperty.value(getProperties());
        }
        return defaultValue;
    }

    private static void setProperty( StringProperty oldProperty,  String value) {
        if (oldProperty != null) oldProperty.set(getProperties(), value);
    }

    private static void setProperty( BooleanProperty oldProperty, boolean value) {
        if (oldProperty != null) oldProperty.set(getProperties(), value);
    }

    public static class State {
         public Boolean DIFF_ENABLED = null;
         public Boolean DIFF_DEFAULT = null;
         public String DIFF_EXE_PATH = null;
         public String DIFF_PARAMETERS = null;

         public Boolean MERGE_ENABLED = null;
         public String MERGE_EXE_PATH = null;
         public String MERGE_PARAMETERS = null;
    }

    public boolean isDiffEnabled() {
        return getProperty(DiffManagerImpl.ENABLE_FILES, myState.DIFF_ENABLED, false);
    }

    public void setDiffEnabled(boolean value) {
        myState.DIFF_ENABLED = value;
    }

    public boolean isDiffDefault() {
        return getProperty(DiffManagerImpl.ENABLE_FILES, myState.DIFF_DEFAULT, false);
    }

    public void setDiffDefault(boolean value) {
        myState.DIFF_DEFAULT = value;
        setProperty(DiffManagerImpl.ENABLE_FILES, value);
    }

    
    public String getDiffExePath() {
        return getProperty(DiffManagerImpl.FILES_TOOL, myState.DIFF_EXE_PATH, "");
    }

    public void setDiffExePath( String path) {
        myState.DIFF_EXE_PATH = path;
        setProperty(DiffManagerImpl.FILES_TOOL, path);
    }

    
    public String getDiffParameters() {
        return getProperty(null, myState.DIFF_PARAMETERS, "%1 %2 %3");
    }

    public void setDiffParameters( String path) {
        myState.DIFF_PARAMETERS = path;
    }


    public boolean isMergeEnabled() {
        return getProperty(DiffManagerImpl.ENABLE_MERGE, myState.MERGE_ENABLED, false);
    }

    public void setMergeEnabled(boolean value) {
        myState.MERGE_ENABLED = value;
        setProperty(DiffManagerImpl.ENABLE_MERGE, value);
    }

    
    public String getMergeExePath() {
        return getProperty(DiffManagerImpl.MERGE_TOOL, myState.MERGE_EXE_PATH, "");
    }

    public void setMergeExePath( String path) {
        myState.MERGE_EXE_PATH = path;
        setProperty(DiffManagerImpl.MERGE_TOOL, path);
    }

    
    public String getMergeParameters() {
        return getProperty(DiffManagerImpl.MERGE_TOOL_PARAMETERS, myState.MERGE_PARAMETERS, "%1 %2 %3 %4");
    }

    public void setMergeParameters( String path) {
        myState.MERGE_PARAMETERS = path;
        setProperty(DiffManagerImpl.MERGE_TOOL_PARAMETERS, path);
    }
}
