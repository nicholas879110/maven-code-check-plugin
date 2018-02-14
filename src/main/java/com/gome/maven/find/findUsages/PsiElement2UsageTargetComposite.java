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
package com.gome.maven.find.findUsages;

import com.gome.maven.find.FindManager;
import com.gome.maven.find.FindSettings;
import com.gome.maven.find.impl.FindManagerImpl;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.usages.UsageInfoToUsageConverter;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Set;

public class PsiElement2UsageTargetComposite extends PsiElement2UsageTargetAdapter {
    private final UsageInfoToUsageConverter.TargetElementsDescriptor myDescriptor;
    public PsiElement2UsageTargetComposite( PsiElement[] primaryElements,
                                            PsiElement[] secondaryElements,
                                            FindUsagesOptions options) {
        super(primaryElements[0], options);
        myDescriptor = new UsageInfoToUsageConverter.TargetElementsDescriptor(primaryElements, secondaryElements);
    }

    @Override
    public void findUsages() {
        PsiElement element = getElement();
        if (element == null) return;
        FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager();
        FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
        boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
        findUsagesManager.findUsages(myDescriptor.getPrimaryElements(), myDescriptor.getAdditionalElements(), handler, myOptions, skipResultsWithOneUsage);
    }

    @Override
    public VirtualFile[] getFiles() {
        Set<VirtualFile> files = ContainerUtil.map2Set(myDescriptor.getAllElements(), new Function<PsiElement, VirtualFile>() {
            @Override
            public VirtualFile fun(PsiElement element) {
                return PsiUtilCore.getVirtualFile(element);
            }
        });
        return VfsUtilCore.toVirtualFileArray(files);
    }

    
    public PsiElement[] getPrimaryElements() {
        return myDescriptor.getPrimaryElements();
    }
    
    public PsiElement[] getSecondaryElements() {
        return myDescriptor.getAdditionalElements();
    }
}