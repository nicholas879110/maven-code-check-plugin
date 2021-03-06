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

package com.gome.maven.ide.util.gotoByName;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.actions.NonProjectScopeDisablerEP;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.ide.util.PsiElementListCellRenderer;
import com.gome.maven.navigation.ChooseByNameContributor;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ex.WindowManagerEx;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.util.indexing.FileBasedIndex;

import java.util.Collection;

/**
 * Model for "Go to | File" action
 */
public class GotoFileModel extends FilteringGotoByModel<FileType> {
    private final int myMaxSize;

    public GotoFileModel( Project project) {
        super(project, Extensions.getExtensions(ChooseByNameContributor.FILE_EP_NAME));
        myMaxSize = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : WindowManagerEx.getInstanceEx().getFrame(project).getSize().width;
    }

    @Override
    protected boolean acceptItem(final NavigationItem item) {
        if (item instanceof PsiFile) {
            final PsiFile file = (PsiFile)item;
            final Collection<FileType> types = getFilterItems();
            // if language substitutors are used, PsiFile.getFileType() can be different from
            // PsiFile.getVirtualFile().getFileType()
            if (types != null) {
                if (types.contains(file.getFileType())) return true;
                VirtualFile vFile = file.getVirtualFile();
                if (vFile != null && types.contains(vFile.getFileType())) return true;
                return false;
            }
            return true;
        }
        else {
            return super.acceptItem(item);
        }
    }

    
    @Override
    protected FileType filterValueFor(NavigationItem item) {
        return item instanceof PsiFile ? ((PsiFile) item).getFileType() : null;
    }

    @Override
    public String getPromptText() {
        return IdeBundle.message("prompt.gotofile.enter.file.name");
    }

    @Override
    public String getCheckBoxName() {
        if (NonProjectScopeDisablerEP.isSearchInNonProjectDisabled()) {
            return null;
        }
        return IdeBundle.message("checkbox.include.non.project.files");
    }

    @Override
    public char getCheckBoxMnemonic() {
        return SystemInfo.isMac?'P':'n';
    }

    @Override
    public String getNotInMessage() {
        return IdeBundle.message("label.no.non.java.files.found");
    }

    @Override
    public String getNotFoundMessage() {
        return IdeBundle.message("label.no.files.found");
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        return propertiesComponent.isTrueValue("GoToClass.toSaveIncludeLibraries") &&
                propertiesComponent.isTrueValue("GoToFile.includeJavaFiles");
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        if (propertiesComponent.isTrueValue("GoToClass.toSaveIncludeLibraries")) {
            propertiesComponent.setValue("GoToFile.includeJavaFiles", Boolean.toString(state));
        }
    }

    @Override
    public PsiElementListCellRenderer getListCellRenderer() {
        return new GotoFileCellRenderer(myMaxSize);
    }

    @Override
    public boolean sameNamesForProjectAndLibraries() {
        return !FileBasedIndex.ourEnableTracingOfKeyHashToVirtualFileMapping;
    }

    @Override
    
    public String getFullName(final Object element) {
        if (element instanceof PsiFileSystemItem) {
            final VirtualFile virtualFile = ((PsiFileSystemItem)element).getVirtualFile();
            return virtualFile != null ? GotoFileCellRenderer.getRelativePath(virtualFile, myProject) : null;
        }

        return getElementName(element);
    }

    @Override
    
    public String[] getSeparators() {
        return new String[] {"/", "\\"};
    }

    @Override
    public String getHelpId() {
        return "procedures.navigating.goto.class";
    }

    @Override
    public boolean willOpenEditor() {
        return true;
    }

    
    @Override
    public String removeModelSpecificMarkup( String pattern) {
        if ((pattern.endsWith("/") || pattern.endsWith("\\"))) {
            return pattern.substring(0, pattern.length() - 1);
        }
        return pattern;
    }
}