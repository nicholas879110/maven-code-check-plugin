/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.usages.impl.rules;

import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.usages.NamedPresentably;
import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.rules.UsageGroupingRule;
import com.gome.maven.usages.rules.UsageInFile;
import com.gome.maven.util.IconUtil;

import javax.swing.*;

/**
 * @author max
 */
public class FileGroupingRule implements UsageGroupingRule {
    private final Project myProject;

    public FileGroupingRule(Project project) {
        myProject = project;
    }

    @Override
    public UsageGroup groupUsage( Usage usage) {
        VirtualFile virtualFile;
        if (usage instanceof UsageInFile && (virtualFile = ((UsageInFile)usage).getFile()) != null) {
            return new FileUsageGroup(myProject, virtualFile);
        }
        return null;
    }

    protected static class FileUsageGroup implements UsageGroup, TypeSafeDataProvider, NamedPresentably {
        private final Project myProject;
        private final VirtualFile myFile;
        private String myPresentableName;
        private Icon myIcon;

        public FileUsageGroup( Project project,  VirtualFile file) {
            myProject = project;
            myFile = file instanceof VirtualFileWindow ? ((VirtualFileWindow)file).getDelegate() : file;
            myPresentableName = myFile.getName();
            update();
        }

        private Icon getIconImpl() {
            return IconUtil.getIcon(myFile, Iconable.ICON_FLAG_READ_STATUS, myProject);
        }

        @Override
        public void update() {
            if (isValid()) {
                myIcon = getIconImpl();
                myPresentableName = myFile.getName();
            }
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FileUsageGroup)) return false;

            final FileUsageGroup fileUsageGroup = (FileUsageGroup)o;

            return myFile.equals(fileUsageGroup.myFile);
        }

        public int hashCode() {
            return myFile.hashCode();
        }

        @Override
        public Icon getIcon(boolean isOpen) {
            return myIcon;
        }

        @Override
        
        public String getText(UsageView view) {
            return myPresentableName;
        }

        @Override
        public FileStatus getFileStatus() {
            return isValid() ? FileStatusManager.getInstance(myProject).getStatus(myFile) : null;
        }

        @Override
        public boolean isValid() {
            return myFile.isValid();
        }

        @Override
        public void navigate(boolean focus) throws UnsupportedOperationException {
            FileEditorManager.getInstance(myProject).openFile(myFile, focus);
        }

        @Override
        public boolean canNavigate() {
            return myFile.isValid();
        }

        @Override
        public boolean canNavigateToSource() {
            return canNavigate();
        }

        @Override
        public int compareTo( UsageGroup usageGroup) {
            return getText(null).compareToIgnoreCase(usageGroup.getText(null));
        }

        @Override
        public void calcData(final DataKey key, final DataSink sink) {
            if (!isValid()) return;
            if (key == CommonDataKeys.VIRTUAL_FILE) {
                sink.put(CommonDataKeys.VIRTUAL_FILE, myFile);
            }
            if (key == CommonDataKeys.PSI_ELEMENT) {
                sink.put(CommonDataKeys.PSI_ELEMENT, getPsiFile());
            }
        }

        
        public PsiFile getPsiFile() {
            return myFile.isValid() ? PsiManager.getInstance(myProject).findFile(myFile) : null;
        }

        @Override
        
        public String getPresentableName() {
            return myPresentableName;
        }
    }
}
