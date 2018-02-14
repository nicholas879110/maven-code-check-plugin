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
package com.gome.maven.usages;

import com.gome.maven.codeInsight.highlighting.ReadWriteAccessDetector;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiElementPointer;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 17, 2005
 */
public class UsageInfoToUsageConverter {
    private UsageInfoToUsageConverter() {
    }

    public static class TargetElementsDescriptor {
        private final List<SmartPsiElementPointer<PsiElement>> myPrimarySearchedElements;
        private final List<SmartPsiElementPointer<PsiElement>> myAdditionalSearchedElements;

        public TargetElementsDescriptor( PsiElement element) {
            this(new PsiElement[]{element});
        }

        public TargetElementsDescriptor( PsiElement[] primarySearchedElements) {
            this(primarySearchedElements, PsiElement.EMPTY_ARRAY);
        }

        public TargetElementsDescriptor( PsiElement[] primarySearchedElements,  PsiElement[] additionalSearchedElements) {
            myPrimarySearchedElements = convertToSmartPointers(primarySearchedElements);
            myAdditionalSearchedElements = convertToSmartPointers(additionalSearchedElements);
        }

        private static final Function<SmartPsiElementPointer<PsiElement>,PsiElement> SMARTPOINTER_TO_ELEMENT_MAPPER = new Function<SmartPsiElementPointer<PsiElement>, PsiElement>() {
            @Override
            public PsiElement fun(final SmartPsiElementPointer<PsiElement> pointer) {
                return pointer.getElement();
            }
        };

        
        private static PsiElement[] convertToPsiElements( List<SmartPsiElementPointer<PsiElement>> primary) {
            return ContainerUtil.toArray(ContainerUtil.mapNotNull(primary, SMARTPOINTER_TO_ELEMENT_MAPPER), PsiElement.ARRAY_FACTORY);
        }

        
        private static List<SmartPsiElementPointer<PsiElement>> convertToSmartPointers( PsiElement[] primaryElements) {
            if (primaryElements.length == 0) return Collections.emptyList();

            final SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(primaryElements[0].getProject());
            return ContainerUtil.mapNotNull(primaryElements, new Function<PsiElement, SmartPsiElementPointer<PsiElement>>() {
                @Override
                public SmartPsiElementPointer<PsiElement> fun(final PsiElement s) {
                    return smartPointerManager.createSmartPsiElementPointer(s);
                }
            });
        }

        /**
         * A read-only attribute describing the target as a "primary" target.
         * A primary target is a target that was the main purpose of the search.
         * All usages of a non-primary target should be considered as a special case of usages of the corresponding primary target.
         * Example: searching field and its getter and setter methods -
         *          the field searched is a primary target, and its accessor methods are non-primary targets, because
         *          for this particular search usages of getter/setter methods are to be considered as a usages of the corresponding field.
         */
        
        public PsiElement[] getPrimaryElements() {
            return convertToPsiElements(myPrimarySearchedElements);
        }

        
        public PsiElement[] getAdditionalElements() {
            return convertToPsiElements(myAdditionalSearchedElements);
        }

        
        public List<PsiElement> getAllElements() {
            List<PsiElement> result = new ArrayList<PsiElement>(myPrimarySearchedElements.size() + myAdditionalSearchedElements.size());
            for (SmartPsiElementPointer pointer : myPrimarySearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            for (SmartPsiElementPointer pointer : myAdditionalSearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }

        
        public List<SmartPsiElementPointer<PsiElement>> getAllElementPointers() {
            List<SmartPsiElementPointer<PsiElement>> result = new ArrayList<SmartPsiElementPointer<PsiElement>>(myPrimarySearchedElements.size() + myAdditionalSearchedElements.size());
            result.addAll(myPrimarySearchedElements);
            result.addAll(myAdditionalSearchedElements);
            return result;
        }
    }

    
    public static Usage convert( TargetElementsDescriptor descriptor,  UsageInfo usageInfo) {
        PsiElement[] primaryElements = descriptor.getPrimaryElements();

        return convert(primaryElements, usageInfo);
    }

    
    public static Usage convert( PsiElement[] primaryElements,  UsageInfo usageInfo) {
        PsiElement usageElement = usageInfo.getElement();
        for(ReadWriteAccessDetector detector: Extensions.getExtensions(ReadWriteAccessDetector.EP_NAME)) {
            if (isReadWriteAccessibleElements(primaryElements, detector)) {
                final ReadWriteAccessDetector.Access rwAccess = detector.getExpressionAccess(usageElement);
                return new ReadWriteAccessUsageInfo2UsageAdapter(usageInfo,
                        rwAccess != ReadWriteAccessDetector.Access.Write,
                        rwAccess != ReadWriteAccessDetector.Access.Read);
            }
        }
        return new UsageInfo2UsageAdapter(usageInfo);
    }

    
    public static Usage[] convert( TargetElementsDescriptor descriptor,  UsageInfo[] usageInfos) {
        Usage[] usages = new Usage[usageInfos.length];
        for (int i = 0; i < usages.length; i++) {
            usages[i] = convert(descriptor, usageInfos[i]);
        }
        return usages;
    }

    
    public static Usage[] convert( final PsiElement[] primaryElements,  UsageInfo[] usageInfos) {
        Usage[] usages = ContainerUtil.map(usageInfos, new Function<UsageInfo, Usage>() {
            @Override
            public Usage fun(UsageInfo info) {
                return convert(primaryElements, info);
            }
        }, new Usage[usageInfos.length]);
        return usages;
    }

    private static boolean isReadWriteAccessibleElements( PsiElement[] elements,  ReadWriteAccessDetector detector) {
        if (elements.length == 0) {
            return false;
        }
        for (PsiElement element : elements) {
            if (!detector.isReadWriteAccessible(element)) return false;
        }
        return true;
    }
}
