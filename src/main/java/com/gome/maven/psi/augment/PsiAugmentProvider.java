/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.psi.augment;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.PsiTypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class PsiAugmentProvider {
    private static final Logger LOG = Logger.getInstance("#" + PsiAugmentProvider.class.getName());
    public static final ExtensionPointName<PsiAugmentProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.lang.psiAugmentProvider");

    
    public abstract <Psi extends PsiElement> List<Psi> getAugments( PsiElement element,  Class<Psi> type);

    
    public static <Psi extends PsiElement> List<Psi> collectAugments( final PsiElement element,  final Class<Psi> type) {
        List<Psi> result = Collections.emptyList();
        for (PsiAugmentProvider provider : Extensions.getExtensions(EP_NAME)) {
            List<Psi> augments = provider.getAugments(element, type);
            if (!augments.isEmpty()) {
                if (result.isEmpty()) result = new ArrayList<Psi>(augments.size());
                result.addAll(augments);
            }
        }

        return result;
    }

    /**
     * Extends {@link PsiTypeElement#getType()} so type could be retrieved from external place
     * e.g. from variable initializer in lombok case (http://projectlombok.org/features/val.html)
     *
     * @param typeElement place where inference takes place, 
     *                    also nested PsiTypeElement-s (e.g. for List<String> PsiTypeElements corresponding to both List and String would be suggested)
     * @return inferred type or null, if inference is not applicable
     *
     * @since 14.1
     */
    
    protected PsiType inferType(PsiTypeElement typeElement) {
        return null;
    }

    
    public static PsiType getInferredType(PsiTypeElement typeElement) {
        for (PsiAugmentProvider provider : Extensions.getExtensions(EP_NAME)) {
            try {
                final PsiType type = provider.inferType(typeElement);
                if (type != null) {
                    return type;
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error("provider: " + provider, e);
            }
        }
        return null;
    }
}
