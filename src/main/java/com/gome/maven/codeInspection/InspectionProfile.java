/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import com.gome.maven.codeInspection.ex.Tools;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.profile.Profile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Consumer;

import java.util.List;

/**
 * User: anna
 * Date: Dec 7, 2004
 */
public interface InspectionProfile extends Profile {

    HighlightDisplayLevel getErrorLevel( HighlightDisplayKey inspectionToolKey, PsiElement element);

    /**
     * If you need to modify tool's settings, please use {@link #modifyToolSettings}
     */
    InspectionToolWrapper getInspectionTool( String shortName,  PsiElement element);

    
    InspectionToolWrapper getInspectionTool( String shortName, Project project);

    /** Returns (unwrapped) inspection */
    InspectionProfileEntry getUnwrappedTool( String shortName,  PsiElement element);

    /** Returns (unwrapped) inspection */
    <T extends InspectionProfileEntry>
    T getUnwrappedTool( Key<T> shortNameKey,  PsiElement element);

    void modifyProfile( Consumer<ModifiableModel> modelConsumer);

    /**
     * Allows a plugin to modify the settings of the inspection tool with the specified ID programmatically, without going through
     * the settings dialog.
     *
     * @param shortNameKey the ID of the tool to change.
     * @param psiElement the element for which the settings should be changed.
     * @param toolConsumer the callback that receives the tool.
     * @since 12.1
     */
    <T extends InspectionProfileEntry>
    void modifyToolSettings( Key<T> shortNameKey,  PsiElement psiElement,  Consumer<T> toolConsumer);

    /**
     * @param element context element
     * @return all (both enabled and disabled) tools
     */
    
    InspectionToolWrapper[] getInspectionTools( PsiElement element);

    void cleanup( Project project);

    /**
     * @see #modifyProfile(com.gome.maven.util.Consumer)
     */
    
    ModifiableModel getModifiableModel();

    boolean isToolEnabled(HighlightDisplayKey key, PsiElement element);

    boolean isToolEnabled(HighlightDisplayKey key);

    boolean isExecutable(Project project);

    boolean isEditable();

    
    String getDisplayName();

    void scopesChanged();

    
    List<Tools> getAllEnabledInspectionTools(Project project);
}
