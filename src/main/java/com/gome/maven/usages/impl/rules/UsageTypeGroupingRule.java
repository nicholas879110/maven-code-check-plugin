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
package com.gome.maven.usages.impl.rules;

import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.usages.*;
import com.gome.maven.usages.rules.PsiElementUsage;
import com.gome.maven.usages.rules.UsageGroupingRuleEx;

import javax.swing.*;

/**
 * @author max
 */
public class UsageTypeGroupingRule implements UsageGroupingRuleEx {
    @Override
    public UsageGroup groupUsage( Usage usage) {
        return groupUsage(usage, UsageTarget.EMPTY_ARRAY);
    }

    @Override
    public UsageGroup groupUsage( Usage usage,  UsageTarget[] targets) {
        if (usage instanceof PsiElementUsage) {
            PsiElementUsage elementUsage = (PsiElementUsage)usage;

            PsiElement element = elementUsage.getElement();
            UsageType usageType = getUsageType(element, targets);

            if (usageType == null && element instanceof PsiFile && elementUsage instanceof UsageInfo2UsageAdapter) {
                usageType = ((UsageInfo2UsageAdapter)elementUsage).getUsageType();
            }

            if (usageType != null) return new UsageTypeGroup(usageType);

            if (usage instanceof ReadWriteAccessUsage) {
                ReadWriteAccessUsage u = (ReadWriteAccessUsage)usage;
                if (u.isAccessedForWriting()) return new UsageTypeGroup(UsageType.WRITE);
                if (u.isAccessedForReading()) return new UsageTypeGroup(UsageType.READ);
            }

            return new UsageTypeGroup(UsageType.UNCLASSIFIED);
        }


        return null;
    }

    
    private static UsageType getUsageType(PsiElement element,  UsageTarget[] targets) {
        if (element == null) return null;

        if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) { return UsageType.COMMENT_USAGE; }

        UsageTypeProvider[] providers = Extensions.getExtensions(UsageTypeProvider.EP_NAME);
        for(UsageTypeProvider provider: providers) {
            UsageType usageType;
            if (provider instanceof UsageTypeProviderEx) {
                usageType = ((UsageTypeProviderEx) provider).getUsageType(element, targets);
            }
            else {
                usageType = provider.getUsageType(element);
            }
            if (usageType != null) {
                return usageType;
            }
        }

        return null;
    }

    private static class UsageTypeGroup implements UsageGroup {
        private final UsageType myUsageType;

        private UsageTypeGroup( UsageType usageType) {
            myUsageType = usageType;
        }

        @Override
        public void update() {
        }

        @Override
        public Icon getIcon(boolean isOpen) {
            return null;
        }

        @Override
        
        public String getText( UsageView view) {
            return view == null ? myUsageType.toString() : myUsageType.toString(view.getPresentation());
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
            if (!(o instanceof UsageTypeGroup)) return false;
            final UsageTypeGroup usageTypeGroup = (UsageTypeGroup)o;
            return myUsageType.equals(usageTypeGroup.myUsageType);
        }

        public int hashCode() {
            return myUsageType.hashCode();
        }
    }
}
