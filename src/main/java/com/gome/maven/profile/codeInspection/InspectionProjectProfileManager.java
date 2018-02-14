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
package com.gome.maven.profile.codeInspection;

import com.gome.maven.codeInspection.InspectionProfile;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.ProjectComponent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.profile.DefaultProjectProfileManager;
import com.gome.maven.profile.Profile;
import com.gome.maven.psi.PsiElement;
import org.jdom.Element;

/**
 * User: anna
 * Date: 30-Nov-2005
 */
public abstract class InspectionProjectProfileManager extends DefaultProjectProfileManager implements ProjectComponent, SeverityProvider, PersistentStateComponent<Element> {
    public InspectionProjectProfileManager( Project project,
                                            InspectionProfileManager inspectionProfileManager,
                                            DependencyValidationManager holder) {
        super(project, inspectionProfileManager, holder);
    }

    public static InspectionProjectProfileManager getInstance(Project project){
        return project.getComponent(InspectionProjectProfileManager.class);
    }

    @Override
    public String getProfileName() {
        return getInspectionProfile().getName();
    }

    
    public InspectionProfile getInspectionProfile(){
        return (InspectionProfile)getProjectProfileImpl();
    }

    /**
     * @deprecated  use {@link #getInspectionProfile()} instead
     */
    @SuppressWarnings({"UnusedDeclaration"})
    
    public InspectionProfile getInspectionProfile(PsiElement element){
        return getInspectionProfile();
    }

    public abstract boolean isProfileLoaded();

    @Override
    
    
    public String getComponentName() {
        return "InspectionProjectProfileManager";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    public abstract void initProfileWrapper( Profile profile);

    @Override
    public Profile getProfile( final String name) {
        return getProfile(name, true);
    }
}
