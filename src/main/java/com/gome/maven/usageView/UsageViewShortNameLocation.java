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

package com.gome.maven.usageView;

import com.gome.maven.lang.findUsages.DescriptiveNameUtil;
import com.gome.maven.psi.ElementDescriptionLocation;
import com.gome.maven.psi.ElementDescriptionProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;

/**
 * @author yole
 */
public class UsageViewShortNameLocation extends ElementDescriptionLocation {
    private UsageViewShortNameLocation() {
    }

    public static final UsageViewShortNameLocation INSTANCE = new UsageViewShortNameLocation();

    @Override
    public ElementDescriptionProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }

    private static final ElementDescriptionProvider DEFAULT_PROVIDER = new ElementDescriptionProvider() {
        @Override
        public String getElementDescription( final PsiElement element,  final ElementDescriptionLocation location) {
            if (!(location instanceof UsageViewShortNameLocation)) return null;

            if (element instanceof PsiMetaOwner) {
                PsiMetaData metaData = ((PsiMetaOwner)element).getMetaData();
                if (metaData!=null) return DescriptiveNameUtil.getMetaDataName(metaData);
            }

            if (element instanceof PsiNamedElement) {
                return ((PsiNamedElement)element).getName();
            }
            return "";
        }
    };
}
