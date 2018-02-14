/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.lang.Language;
import com.gome.maven.lang.findUsages.DescriptiveNameUtil;
import com.gome.maven.lang.findUsages.FindUsagesProvider;
import com.gome.maven.lang.findUsages.LanguageFindUsages;
import com.gome.maven.psi.ElementDescriptionLocation;
import com.gome.maven.psi.ElementDescriptionProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.meta.PsiPresentableMetaData;

/**
 * @author peter
 */
public class UsageViewNodeTextLocation extends ElementDescriptionLocation {
    private UsageViewNodeTextLocation() { }

    public static final UsageViewNodeTextLocation INSTANCE = new UsageViewNodeTextLocation();

    @Override
    public ElementDescriptionProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }

    private static final ElementDescriptionProvider DEFAULT_PROVIDER = new ElementDescriptionProvider() {
        @Override
        public String getElementDescription( final PsiElement element,  final ElementDescriptionLocation location) {
            if (!(location instanceof UsageViewNodeTextLocation)) return null;

            if (element instanceof PsiMetaOwner) {
                final PsiMetaData metaData = ((PsiMetaOwner)element).getMetaData();
                if (metaData instanceof PsiPresentableMetaData) {
                    return ((PsiPresentableMetaData)metaData).getTypeName() + " " + DescriptiveNameUtil.getMetaDataName(metaData);
                }
            }

            if (element instanceof PsiFile) {
                return ((PsiFile)element).getName();
            }

            Language language = element.getLanguage();
            FindUsagesProvider provider = LanguageFindUsages.INSTANCE.forLanguage(language);
            assert provider != null : "Element: " + element + ", language: " + language;
            return provider.getNodeText(element, true);
        }
    };
}