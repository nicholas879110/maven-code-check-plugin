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

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.rules.PsiElementUsage;
import com.gome.maven.usages.rules.UsageGroupingRule;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

/**
 * @author max
 */
public class UsageScopeGroupingRule implements UsageGroupingRule {
    @Override
    public UsageGroup groupUsage( Usage usage) {
        if (!(usage instanceof PsiElementUsage)) {
            return null;
        }
        PsiElementUsage elementUsage = (PsiElementUsage)usage;

        PsiElement element = elementUsage.getElement();
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);

        if (virtualFile == null) {
            return null;
        }
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
        boolean isInLib = fileIndex.isInLibraryClasses(virtualFile) || fileIndex.isInLibrarySource(virtualFile);
        if (isInLib) return LIBRARY;
        boolean isInTest = fileIndex.isInTestSourceContent(virtualFile);
        return isInTest ? TEST : PRODUCTION;
    }

    private static final UsageScopeGroup TEST = new UsageScopeGroup(0) {
        @Override
        public Icon getIcon(boolean isOpen) {
            return AllIcons.Modules.TestSourceFolder;
        }

        @Override
        
        public String getText(UsageView view) {
            return "Test";
        }
    };
    private static final UsageScopeGroup PRODUCTION = new UsageScopeGroup(1) {
        @Override
        public Icon getIcon(boolean isOpen) {
            return PlatformIcons.SOURCE_FOLDERS_ICON;
        }

        @Override
        
        public String getText(UsageView view) {
            return "Production";
        }
    };
    private static final UsageScopeGroup LIBRARY = new UsageScopeGroup(2) {
        @Override
        public Icon getIcon(boolean isOpen) {
            return PlatformIcons.LIBRARY_ICON;
        }

        @Override
        
        public String getText(UsageView view) {
            return "Library";
        }
    };
    private abstract static class UsageScopeGroup implements UsageGroup {
        private final int myCode;

        private UsageScopeGroup(int code) {
            myCode = code;
        }

        @Override
        public void update() {
        }

        @Override
        public FileStatus getFileStatus() {
            return null;
        }

        @Override
        public boolean isValid() { return true; }
        @Override
        public void navigate(boolean focus) { }
        @Override
        public boolean canNavigate() { return false; }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public int compareTo( UsageGroup usageGroup) {
            return getText(null).compareTo(usageGroup.getText(null));
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UsageScopeGroup)) return false;
            final UsageScopeGroup usageTypeGroup = (UsageScopeGroup)o;
            return myCode == usageTypeGroup.myCode;
        }

        public int hashCode() {
            return myCode;
        }
    }
}
