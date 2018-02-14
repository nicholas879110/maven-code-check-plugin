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

package com.gome.maven.refactoring.rename;

import com.gome.maven.refactoring.util.NonCodeUsageInfo;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.usageView.UsageInfoFactory;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.codeInsight.TargetElementUtilBase;

public class NonCodeUsageInfoFactory implements UsageInfoFactory {
    private final PsiElement myElement;
    private final String myStringToReplace;

    public NonCodeUsageInfoFactory(final PsiElement element, final String stringToReplace) {
        myElement = element;
        myStringToReplace = stringToReplace;
    }

    @Override
    
    public UsageInfo createUsageInfo( PsiElement usage, int startOffset, int endOffset) {
        final PsiElement namedElement = TargetElementUtilBase.getInstance().getNamedElement(usage, startOffset);
        if (namedElement != null) {
            return null;
        }

        int start = usage.getTextRange().getStartOffset();
        return NonCodeUsageInfo.create(usage.getContainingFile(), start + startOffset, start + endOffset, myElement, myStringToReplace);
    }
}