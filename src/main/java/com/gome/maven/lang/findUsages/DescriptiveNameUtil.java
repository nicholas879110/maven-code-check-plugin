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
package com.gome.maven.lang.findUsages;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;

public class DescriptiveNameUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.lang.findUsages.DescriptiveNameUtil");

    public static String getMetaDataName(final PsiMetaData metaData) {
        final String name = metaData.getName();
        return StringUtil.isEmpty(name) ? "''" : name;
    }

    public static String getDescriptiveName( PsiElement psiElement) {
        LOG.assertTrue(psiElement.isValid());

        if (psiElement instanceof PsiMetaOwner) {
            final PsiMetaOwner psiMetaOwner = (PsiMetaOwner)psiElement;
            final PsiMetaData metaData = psiMetaOwner.getMetaData();
            if (metaData != null) return getMetaDataName(metaData);
        }

        final Language lang = psiElement.getLanguage();
        final FindUsagesProvider provider = LanguageFindUsages.INSTANCE.forLanguage(lang);
        assert provider != null : lang;
        return provider.getDescriptiveName(psiElement);
    }
}
