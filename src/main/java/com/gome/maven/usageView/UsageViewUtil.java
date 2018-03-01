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

package com.gome.maven.usageView;

import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.GeneratedSourcesFilter;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.psi.ElementDescriptionUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.util.MoveRenameUsageInfo;
import com.gome.maven.refactoring.util.NonCodeUsageInfo;
import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageInfo2UsageAdapter;
import com.gome.maven.usages.UsageView;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class UsageViewUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.usageView.UsageViewUtil");

    private UsageViewUtil() { }

    public static String createNodeText(PsiElement element) {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewNodeTextLocation.INSTANCE);
    }

    public static String getShortName(final PsiElement psiElement) {
        LOG.assertTrue(psiElement.isValid(), psiElement);
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewShortNameLocation.INSTANCE);
    }

    public static String getLongName(final PsiElement psiElement) {
        LOG.assertTrue(psiElement.isValid(), psiElement);
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewLongNameLocation.INSTANCE);
    }

    public static String getType( PsiElement psiElement) {
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewTypeLocation.INSTANCE);
    }

    public static boolean hasNonCodeUsages(UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            if (usage.isNonCodeUsage) return true;
        }
        return false;
    }

    public static boolean hasUsagesInGeneratedCode(UsageInfo[] usages, Project project) {
        GeneratedSourcesFilter[] filters = GeneratedSourcesFilter.EP_NAME.getExtensions();
        for (UsageInfo usage : usages) {
            VirtualFile file = usage.getVirtualFile();
            if (file != null) {
                for (GeneratedSourcesFilter filter : filters) {
                    if (filter.isGeneratedSource(file, project)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean hasReadOnlyUsages(UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            if (!usage.isWritable()) return true;
        }
        return false;
    }

    public static UsageInfo[] removeDuplicatedUsages( UsageInfo[] usages) {
        Set<UsageInfo> set = new LinkedHashSet<UsageInfo>(Arrays.asList(usages));

        // Replace duplicates of move rename usage infos in injections from non code usages of master files
        String newTextInNonCodeUsage = null;

        for(UsageInfo usage:usages) {
            if (!(usage instanceof NonCodeUsageInfo)) continue;
            newTextInNonCodeUsage = ((NonCodeUsageInfo)usage).newText;
            break;
        }

        if (newTextInNonCodeUsage != null) {
            for(UsageInfo usage:usages) {
                if (!(usage instanceof MoveRenameUsageInfo)) continue;
                PsiFile file = usage.getFile();

                if (file != null) {
                    PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
                    if (context != null) {

                        PsiElement usageElement = usage.getElement();
                        if (usageElement == null) continue;

                        PsiReference psiReference = usage.getReference();
                        if (psiReference == null) continue;

                        int injectionOffsetInMasterFile = InjectedLanguageManager.getInstance(usageElement.getProject()).injectedToHost(usageElement, usageElement.getTextOffset());
                        TextRange rangeInElement = usage.getRangeInElement();
                        assert rangeInElement != null : usage;
                        TextRange range = rangeInElement.shiftRight(injectionOffsetInMasterFile);
                        PsiFile containingFile = context.getContainingFile();
                        if (containingFile == null) continue; //move package to another package
                        set.remove(
                                NonCodeUsageInfo.create(
                                        containingFile,
                                        range.getStartOffset(),
                                        range.getEndOffset(),
                                        ((MoveRenameUsageInfo)usage).getReferencedElement(),
                                        newTextInNonCodeUsage
                                )
                        );
                    }
                }
            }
        }
        return set.toArray(new UsageInfo[set.size()]);
    }

    
    public static UsageInfo[] toUsageInfoArray( final Collection<? extends UsageInfo> collection) {
        final int size = collection.size();
        return size == 0 ? UsageInfo.EMPTY_ARRAY : collection.toArray(new UsageInfo[size]);
    }

    
    public static PsiElement[] toElements( UsageInfo[] usageInfos) {
        return ContainerUtil.map2Array(usageInfos, PsiElement.class, new Function<UsageInfo, PsiElement>() {
            @Override
            public PsiElement fun(UsageInfo info) {
                return info.getElement();
            }
        });
    }

    public static void navigateTo( UsageInfo info, boolean requestFocus) {
        int offset = info.getNavigationOffset();
        VirtualFile file = info.getVirtualFile();
        Project project = info.getProject();
        if (file != null) {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file, offset), requestFocus);
        }
    }

    public static Set<UsageInfo> getNotExcludedUsageInfos(final UsageView usageView) {
        Set<Usage> excludedUsages = usageView.getExcludedUsages();

        Set<UsageInfo> usageInfos = new LinkedHashSet<UsageInfo>();
        for (Usage usage : usageView.getUsages()) {
            if (usage instanceof UsageInfo2UsageAdapter && !excludedUsages.contains(usage)) {
                UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();
                usageInfos.add(usageInfo);
            }
        }
        return usageInfos;
    }

    public static boolean reportNonRegularUsages(UsageInfo[] usages, final Project project) {
        boolean inGeneratedCode = hasUsagesInGeneratedCode(usages, project);
        if (hasNonCodeUsages(usages) || inGeneratedCode) {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(inGeneratedCode ? RefactoringBundle.message("occurrences.found.in.comments.strings.non.java.files.and.generated.code")
                        : RefactoringBundle.message("occurrences.found.in.comments.strings.and.non.java.files"));
            }
            return true;
        }
        return false;
    }
}
