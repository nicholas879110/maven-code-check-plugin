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

package com.gome.maven.codeInspection;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlSuppressionProvider implements InspectionSuppressor {

    public static final ExtensionPointName<XmlSuppressionProvider> EP_NAME = new ExtensionPointName<XmlSuppressionProvider>("com.gome.maven.xml.xmlSuppressionProvider");

    public static boolean isSuppressed( PsiElement element,  String inspectionId) {
        for (XmlSuppressionProvider provider : Extensions.getExtensions(EP_NAME)) {
            if (provider.isProviderAvailable(element.getContainingFile()) && provider.isSuppressedFor(element, inspectionId)) {
                return true;
            }
        }
        return false;
    }

    public abstract boolean isProviderAvailable( PsiFile file);

    @Override
    public abstract boolean isSuppressedFor( PsiElement element,  String inspectionId);

    public abstract void suppressForFile( PsiElement element,  String inspectionId);

    public abstract void suppressForTag( PsiElement element,  String inspectionId);

    
    @Override
    public SuppressQuickFix[] getSuppressActions( PsiElement element,  String toolShortName) {
        return XmlSuppressableInspectionTool.getSuppressFixes(toolShortName, this);
    }


}
