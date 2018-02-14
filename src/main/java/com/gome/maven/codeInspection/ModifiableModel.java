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

package com.gome.maven.codeInspection;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.Profile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;

import java.io.IOException;

/**
 * User: anna
 * Date: 15-Feb-2006
 */
public interface ModifiableModel extends Profile {

    InspectionProfile getParentProfile();

    
    String getBaseProfileName();

    void setBaseProfile(InspectionProfile profile);

    void enableTool( String inspectionTool, NamedScope namedScope, Project project);

    void disableTool( String inspectionTool, NamedScope namedScope,  Project project);

    void setErrorLevel(HighlightDisplayKey key,  HighlightDisplayLevel level, Project project);

    HighlightDisplayLevel getErrorLevel(HighlightDisplayKey inspectionToolKey, PsiElement element);

    boolean isToolEnabled(HighlightDisplayKey key);

    boolean isToolEnabled(HighlightDisplayKey key, PsiElement element);

    void commit() throws IOException;

    boolean isChanged();

    void setModified(final boolean toolsSettingsChanged);

    boolean isProperSetting( String toolId);

    void resetToBase(Project project);

    void resetToEmpty(Project project);

    /**
     * @return {@link com.gome.maven.codeInspection.ex.InspectionToolWrapper}
     * @see #getUnwrappedTool(String, com.gome.maven.psi.PsiElement)
     */
    InspectionToolWrapper getInspectionTool(String shortName, PsiElement element);

    InspectionProfileEntry getUnwrappedTool( String shortName,  PsiElement element);

    InspectionToolWrapper[] getInspectionTools(PsiElement element);

    void copyFrom(InspectionProfile profile);

    void setEditable(String toolDisplayName);

    void save() throws IOException;

    boolean isProfileLocked();

    void lockProfile(boolean isLocked);

    void disableTool( String toolId,  PsiElement element);

    void disableTool( String inspectionTool, Project project);
}
