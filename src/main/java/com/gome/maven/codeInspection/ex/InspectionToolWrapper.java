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
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInspection.CleanupLocalInspectionTool;
import com.gome.maven.codeInspection.GlobalInspectionContext;
import com.gome.maven.codeInspection.InspectionEP;
import com.gome.maven.codeInspection.InspectionProfileEntry;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.ResourceUtil;

import java.io.IOException;
import java.net.URL;

/**
 * @author Dmitry Avdeev
 *         Date: 9/28/11
 */
public abstract class InspectionToolWrapper<T extends InspectionProfileEntry, E extends InspectionEP> {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ex.InspectionToolWrapper");

    protected T myTool;
    protected final E myEP;

    protected InspectionToolWrapper( E ep) {
        this(null, ep);
    }

    protected InspectionToolWrapper( T tool) {
        this(tool, null);
    }

    protected InspectionToolWrapper( T tool,  E ep) {
        assert tool != null || ep != null : "must not be both null";
        myEP = ep;
        myTool = tool;
    }

    /** Copy ctor */
    protected InspectionToolWrapper( InspectionToolWrapper<T, E> other) {
        myEP = other.myEP;
        // we need to create a copy for buffering
        //noinspection unchecked
        myTool = other.myTool == null ? null : (T)InspectionToolsRegistrarCore.instantiateTool(other.myTool.getClass());
    }

    public void initialize( GlobalInspectionContext context) {
        projectOpened(context.getProject());
    }

    
    public abstract InspectionToolWrapper<T, E> createCopy();

    
    public T getTool() {
        T tool = myTool;
        if (tool == null) {
            //noinspection unchecked
            myTool = tool = (T)myEP.instantiateTool();
            if (!tool.getShortName().equals(myEP.getShortName())) {
                LOG.error("Short name not matched for " + tool.getClass() + ": getShortName() = " + tool.getShortName() + "; ep.shortName = " + myEP.getShortName());
            }
        }
        return tool;
    }

    public boolean isInitialized() {
        return myTool != null;
    }

    /**
     * @see #applyToDialects()
     * @see #isApplicable(com.gome.maven.lang.Language)
     */
    
    public String getLanguage() {
        return myEP == null ? null : myEP.language;
    }

    public boolean applyToDialects() {
        return myEP != null && myEP.applyToDialects;
    }

    public boolean isApplicable( Language language) {
        String langId = getLanguage();
        return langId == null || language.getID().equals(langId) || applyToDialects() && language.isKindOf(langId);
    }

    public boolean isCleanupTool() {
        return myEP != null ? myEP.cleanupTool : getTool() instanceof CleanupLocalInspectionTool;
    }

    
    public String getShortName() {
        return myEP != null ? myEP.getShortName() : getTool().getShortName();
    }

    
    public String getDisplayName() {
        if (myEP == null) {
            return getTool().getDisplayName();
        }
        else {
            String name = myEP.getDisplayName();
            return name == null ? getTool().getDisplayName() : name;
        }
    }

    
    public String getGroupDisplayName() {
        if (myEP == null) {
            return getTool().getGroupDisplayName();
        }
        else {
            String groupDisplayName = myEP.getGroupDisplayName();
            return groupDisplayName == null ? getTool().getGroupDisplayName() : groupDisplayName;
        }
    }

    public boolean isEnabledByDefault() {
        return myEP == null ? getTool().isEnabledByDefault() : myEP.enabledByDefault;
    }

    
    public HighlightDisplayLevel getDefaultLevel() {
        return myEP == null ? getTool().getDefaultLevel() : myEP.getDefaultLevel();
    }

    
    public String[] getGroupPath() {
        if (myEP == null) {
            return getTool().getGroupPath();
        }
        else {
            String[] path = myEP.getGroupPath();
            return path == null ? getTool().getGroupPath() : path;
        }
    }

    public void projectOpened( Project project) {
        if (myEP == null) {
            getTool().projectOpened(project);
        }
    }

    public void projectClosed( Project project) {
        if (myEP == null) {
            getTool().projectClosed(project);
        }
    }

    public String getStaticDescription() {
        return myEP == null || myEP.hasStaticDescription ? getTool().getStaticDescription() : null;
    }

    public String loadDescription() {
        final String description = getStaticDescription();
        if (description != null) return description;
        try {
            URL descriptionUrl = getDescriptionUrl();
            if (descriptionUrl == null) return null;
            return ResourceUtil.loadText(descriptionUrl);
        }
        catch (IOException ignored) { }

        return getTool().loadDescription();
    }

    protected URL getDescriptionUrl() {
        Application app = ApplicationManager.getApplication();
        if (myEP == null || app.isUnitTestMode() || app.isHeadlessEnvironment()) {
            return superGetDescriptionUrl();
        }
        String fileName = getDescriptionFileName();
        return myEP.getLoaderForClass().getResource("/inspectionDescriptions/" + fileName);
    }

    
    protected URL superGetDescriptionUrl() {
        final String fileName = getDescriptionFileName();
        return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
    }

    
    public String getDescriptionFileName() {
        return getShortName() + ".html";
    }

    
    public final String getFolderName() {
        return getShortName();
    }

    
    public Class<? extends InspectionProfileEntry> getDescriptionContextClass() {
        return getTool().getClass();
    }

    public String getMainToolId() {
        return getTool().getMainToolId();
    }

    public E getExtension() {
        return myEP;
    }

    @Override
    public String toString() {
        return getShortName();
    }

    public void cleanup(Project project) {
        T tool = myTool;
        if (tool != null) {
            tool.cleanup(project);
        }
    }

    
    public abstract JobDescriptor[] getJobDescriptors( GlobalInspectionContext context);
}
