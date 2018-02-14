/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.daemon.impl.analysis;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectCoreUtil;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.SingleRootFileViewProvider;
import com.gome.maven.psi.search.ProjectScope;
import com.gome.maven.psi.util.PsiUtilBase;
import org.jdom.Element;

import java.util.*;

@State(name="HighlightingSettingsPerFile", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class HighlightingSettingsPerFile extends HighlightingLevelManager implements PersistentStateComponent<Element> {
     private static final String SETTING_TAG = "setting";
     private static final String ROOT_ATT_PREFIX = "root";
     private static final String FILE_ATT = "file";

    public static HighlightingSettingsPerFile getInstance(Project project){
        return (HighlightingSettingsPerFile)ServiceManager.getService(project, HighlightingLevelManager.class);
    }

    private final Map<VirtualFile, FileHighlightingSetting[]> myHighlightSettings = new HashMap<VirtualFile, FileHighlightingSetting[]>();

    
    public FileHighlightingSetting getHighlightingSettingForRoot( PsiElement root){
        final PsiFile containingFile = root.getContainingFile();
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        FileHighlightingSetting[] fileHighlightingSettings = myHighlightSettings.get(virtualFile);
        final int index = PsiUtilBase.getRootIndex(root);

        if(fileHighlightingSettings == null || fileHighlightingSettings.length <= index) {
            return getDefaultHighlightingSetting(root.getProject(), virtualFile);
        }
        return fileHighlightingSettings[index];
    }

    
    private static FileHighlightingSetting getDefaultHighlightingSetting( Project project, final VirtualFile virtualFile) {
        if (virtualFile != null) {
            DefaultHighlightingSettingProvider[] providers = DefaultHighlightingSettingProvider.EP_NAME.getExtensions();
            List<DefaultHighlightingSettingProvider> filtered = DumbService.getInstance(project).filterByDumbAwareness(providers);
            for (DefaultHighlightingSettingProvider p : filtered) {
                FileHighlightingSetting setting = p.getDefaultSetting(project, virtualFile);
                if (setting != null) {
                    return setting;
                }
            }
        }
        return FileHighlightingSetting.FORCE_HIGHLIGHTING;
    }

    
    private static FileHighlightingSetting[] getDefaults( PsiFile file){
        final int rootsCount = file.getViewProvider().getLanguages().size();
        final FileHighlightingSetting[] fileHighlightingSettings = new FileHighlightingSetting[rootsCount];
        for (int i = 0; i < fileHighlightingSettings.length; i++) {
            fileHighlightingSettings[i] = FileHighlightingSetting.FORCE_HIGHLIGHTING;
        }
        return fileHighlightingSettings;
    }

    public void setHighlightingSettingForRoot( PsiElement root,  FileHighlightingSetting setting) {
        final PsiFile containingFile = root.getContainingFile();
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) return;
        FileHighlightingSetting[] defaults = myHighlightSettings.get(virtualFile);
        int rootIndex = PsiUtilBase.getRootIndex(root);
        if (defaults != null && rootIndex >= defaults.length) defaults = null;
        if (defaults == null) defaults = getDefaults(containingFile);
        defaults[rootIndex] = setting;
        boolean toRemove = true;
        for (FileHighlightingSetting aDefault : defaults) {
            if (aDefault != FileHighlightingSetting.NONE) toRemove = false;
        }
        if (toRemove) {
            myHighlightSettings.remove(virtualFile);
        }
        else {
            myHighlightSettings.put(virtualFile, defaults);
        }
    }

    @Override
    public void loadState(Element element) {
        List children = element.getChildren(SETTING_TAG);
        for (final Object aChildren : children) {
            final Element child = (Element)aChildren;
            final String url = child.getAttributeValue(FILE_ATT);
            if (url == null) continue;
            final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(url);
            if (fileByUrl != null) {
                final List<FileHighlightingSetting> settings = new ArrayList<FileHighlightingSetting>();
                int index = 0;
                while (child.getAttributeValue(ROOT_ATT_PREFIX + index) != null) {
                    final String attributeValue = child.getAttributeValue(ROOT_ATT_PREFIX + index++);
                    settings.add(Enum.valueOf(FileHighlightingSetting.class, attributeValue));
                }
                myHighlightSettings.put(fileByUrl, settings.toArray(new FileHighlightingSetting[settings.size()]));
            }
        }
    }

    @Override
    public Element getState() {
        final Element element = new Element("state");
        for (Map.Entry<VirtualFile, FileHighlightingSetting[]> entry : myHighlightSettings.entrySet()) {
            final Element child = new Element(SETTING_TAG);

            final VirtualFile vFile = entry.getKey();
            if (!vFile.isValid()) continue;
            child.setAttribute(FILE_ATT, vFile.getUrl());
            for (int i = 0; i < entry.getValue().length; i++) {
                final FileHighlightingSetting fileHighlightingSetting = entry.getValue()[i];
                child.setAttribute(ROOT_ATT_PREFIX + i, fileHighlightingSetting.toString());
            }
            element.addContent(child);
        }
        return element;
    }

    @Override
    public boolean shouldHighlight( PsiElement psiRoot) {
        final FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
        return settingForRoot != FileHighlightingSetting.SKIP_HIGHLIGHTING;
    }

    @Override
    public boolean shouldInspect( PsiElement psiRoot) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return true;

        final FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
        if (settingForRoot == FileHighlightingSetting.SKIP_HIGHLIGHTING ||
                settingForRoot == FileHighlightingSetting.SKIP_INSPECTION) {
            return false;
        }
        final Project project = psiRoot.getProject();
        final VirtualFile virtualFile = psiRoot.getContainingFile().getVirtualFile();
        if (virtualFile == null || !virtualFile.isValid()) return false;

        if (ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile)) return false;

        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (ProjectScope.getLibrariesScope(project).contains(virtualFile) && !fileIndex.isInContent(virtualFile)) return false;

        return !SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile);
    }
}
