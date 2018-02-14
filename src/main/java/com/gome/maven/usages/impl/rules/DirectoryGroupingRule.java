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
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.DataSink;
import com.gome.maven.openapi.actionSystem.TypeSafeDataProvider;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.rules.UsageGroupingRule;
import com.gome.maven.usages.rules.UsageInFile;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;
import java.io.File;

/**
 * @author yole
 */
public class DirectoryGroupingRule implements UsageGroupingRule {
    public static DirectoryGroupingRule getInstance(Project project) {
        return ServiceManager.getService(project, DirectoryGroupingRule.class);
    }

    protected final Project myProject;

    public DirectoryGroupingRule(Project project) {
        myProject = project;
    }

    @Override
    
    public UsageGroup groupUsage( Usage usage) {
        if (usage instanceof UsageInFile) {
            UsageInFile usageInFile = (UsageInFile)usage;
            VirtualFile file = usageInFile.getFile();
            if (file != null) {
                if (file instanceof VirtualFileWindow) {
                    file = ((VirtualFileWindow)file).getDelegate();
                }
                VirtualFile dir = file.getParent();
                if (dir == null) return null;
                return getGroupForFile(dir);
            }
        }
        return null;
    }

    protected UsageGroup getGroupForFile( VirtualFile dir) {
        return new DirectoryGroup(dir);
    }

    public String getActionTitle() {
        return "Group by directory";
    }

    private class DirectoryGroup implements UsageGroup, TypeSafeDataProvider {
        private final VirtualFile myDir;

        @Override
        public void update() {
        }

        private DirectoryGroup( VirtualFile dir) {
            myDir = dir;
        }

        @Override
        public Icon getIcon(boolean isOpen) {
            return PlatformIcons.DIRECTORY_CLOSED_ICON;
        }

        @Override
        
        public String getText(UsageView view) {
            VirtualFile baseDir = myProject.getBaseDir();
            String relativePath = baseDir == null ? null : VfsUtilCore.getRelativePath(myDir, baseDir, File.separatorChar);
            return relativePath == null ? myDir.getPresentableUrl() : relativePath;
        }

        @Override
        public FileStatus getFileStatus() {
            return isValid() ? FileStatusManager.getInstance(myProject).getStatus(myDir) : null;
        }

        @Override
        public boolean isValid() {
            return myDir.isValid();
        }

        @Override
        public void navigate(boolean focus) throws UnsupportedOperationException {
            final PsiDirectory directory = getDirectory();
            if (directory != null && directory.canNavigate()) {
                directory.navigate(focus);
            }
        }

        private PsiDirectory getDirectory() {
            return myDir.isValid() ? PsiManager.getInstance(myProject).findDirectory(myDir) : null;
        }
        @Override
        public boolean canNavigate() {
            final PsiDirectory directory = getDirectory();
            return directory != null && directory.canNavigate();
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public int compareTo( UsageGroup usageGroup) {
            return getText(null).compareToIgnoreCase(usageGroup.getText(null));
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DirectoryGroup)) return false;
            return myDir.equals(((DirectoryGroup)o).myDir);
        }

        public int hashCode() {
            return myDir.hashCode();
        }

        @Override
        public void calcData(final DataKey key, final DataSink sink) {
            if (!isValid()) return;
            if (CommonDataKeys.VIRTUAL_FILE == key) {
                sink.put(CommonDataKeys.VIRTUAL_FILE, myDir);
            }
            if (CommonDataKeys.PSI_ELEMENT == key) {
                sink.put(CommonDataKeys.PSI_ELEMENT, getDirectory());
            }
        }
    }
}
