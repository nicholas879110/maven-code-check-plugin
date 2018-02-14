/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.refactoring.rename;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.usageView.UsageViewBundle;
import com.gome.maven.usageView.UsageViewDescriptor;
import com.gome.maven.usageView.UsageViewUtil;
import com.gome.maven.util.ArrayUtil;
import gnu.trove.THashSet;

import java.util.LinkedHashMap;
import java.util.Set;

public class RenameViewDescriptor implements UsageViewDescriptor{
    private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.rename.RenameViewDescriptor");
    private final String myProcessedElementsHeader;
    private final String myCodeReferencesText;
    private final PsiElement[] myElements;

    public RenameViewDescriptor(LinkedHashMap<PsiElement, String> renamesMap) {

        myElements = PsiUtilCore.toPsiElementArray(renamesMap.keySet());

        Set<String> processedElementsHeaders = new THashSet<String>();
        Set<String> codeReferences = new THashSet<String>();

        for (final PsiElement element : myElements) {
            LOG.assertTrue(element.isValid(), "Invalid element: " + element.toString());
            String newName = renamesMap.get(element);

            String prefix = "";
            if (element instanceof PsiDirectory/* || element instanceof PsiClass*/) {
                String fullName = UsageViewUtil.getLongName(element);
                int lastDot = fullName.lastIndexOf('.');
                if (lastDot >= 0) {
                    prefix = fullName.substring(0, lastDot + 1);
                }
            }

            processedElementsHeaders.add(StringUtil.capitalize(
                    RefactoringBundle.message("0.to.be.renamed.to.1.2", UsageViewUtil.getType(element), prefix, newName)));
            codeReferences.add(UsageViewUtil.getType(element) + " " + UsageViewUtil.getLongName(element));
        }


        myProcessedElementsHeader = StringUtil.join(ArrayUtil.toStringArray(processedElementsHeaders), ", ");
        myCodeReferencesText =  RefactoringBundle.message("references.in.code.to.0", StringUtil.join(ArrayUtil.toStringArray(codeReferences),
                ", "));
    }

    @Override

    public PsiElement[] getElements() {
        return myElements;
    }

    @Override
    public String getProcessedElementsHeader() {
        return myProcessedElementsHeader;
    }

    @Override
    public String getCodeReferencesText(int usagesCount, int filesCount) {
        return myCodeReferencesText + UsageViewBundle.getReferencesString(usagesCount, filesCount);
    }

    @Override
    public String getCommentReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("comments.elements.header",
                UsageViewBundle.getOccurencesString(usagesCount, filesCount));
    }

}