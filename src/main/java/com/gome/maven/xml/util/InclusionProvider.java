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
package com.gome.maven.xml.util;

import com.gome.maven.openapi.util.NullableComputable;
import com.gome.maven.openapi.util.RecursionManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.CachedValueProvider;
import com.gome.maven.psi.util.CachedValuesManager;
import com.gome.maven.psi.util.PsiModificationTracker;
import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.xmlb.JDOMXIncluder;

import java.util.regex.Matcher;

/**
 * @author peter
 */
class InclusionProvider implements CachedValueProvider<PsiElement[]> {
    private final XmlTag myXincludeTag;

    public InclusionProvider(XmlTag xincludeTag) {
        myXincludeTag = xincludeTag;
    }

    
    public static PsiElement[] getIncludedTags(XmlTag xincludeTag) {
        if (XmlUtil.isStubBuilding()) return PsiElement.EMPTY_ARRAY;
        return CachedValuesManager.getCachedValue(xincludeTag, new InclusionProvider(xincludeTag));
    }

    @Override
    public Result<PsiElement[]> compute() {
        PsiElement[] result = RecursionManager.doPreventingRecursion(myXincludeTag, true, new NullableComputable<PsiElement[]>() {
            @Override
            public PsiElement[] compute() {
                return computeInclusion(myXincludeTag);
            }
        });
        return Result.create(result == null ? PsiElement.EMPTY_ARRAY : result, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
    }

    private static XmlTag[] extractXpointer( XmlTag rootTag,  final String xpointer) {
        if (xpointer != null) {
            Matcher matcher = JDOMXIncluder.XPOINTER_PATTERN.matcher(xpointer);
            if (matcher.matches()) {
                String pointer = matcher.group(1);
                matcher = JDOMXIncluder.CHILDREN_PATTERN.matcher(pointer);
                if (matcher.matches() && matcher.group(1).equals(rootTag.getName())) {
                    return rootTag.getSubTags();
                }
            }
        }

        return new XmlTag[]{rootTag};
    }

    
    private static PsiElement[] computeInclusion(final XmlTag xincludeTag) {
        final XmlFile included = XmlIncludeHandler.resolveXIncludeFile(xincludeTag);
        final XmlDocument document = included != null ? included.getDocument() : null;
        final XmlTag rootTag = document != null ? document.getRootTag() : null;
        if (rootTag != null) {
            final String xpointer = xincludeTag.getAttributeValue("xpointer", XmlPsiUtil.XINCLUDE_URI);
            final XmlTag[] includeTag = extractXpointer(rootTag, xpointer);
            PsiElement[] result = new PsiElement[includeTag.length];
            for (int i = 0; i < includeTag.length; i++) {
                result[i] = new IncludedXmlTag(includeTag[i], xincludeTag.getParentTag());
            }
            return result;
        }

        return null;
    }

}
