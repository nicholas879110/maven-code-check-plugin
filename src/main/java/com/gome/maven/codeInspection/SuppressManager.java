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

/*
 * User: anna
 * Date: 24-Dec-2007
 */
package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.psi.PsiAnnotation;
import com.gome.maven.psi.PsiCodeBlock;
import com.gome.maven.psi.PsiField;
import com.gome.maven.psi.PsiLiteralExpression;
import com.gome.maven.psi.util.PsiTreeUtil;

public abstract class SuppressManager implements BatchSuppressManager, InspectionSuppressor {

    public static SuppressManager getInstance() {
        return ServiceManager.getService(SuppressManager.class);
    }

    public static boolean isSuppressedInspectionName(PsiLiteralExpression expression) {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(expression, PsiAnnotation.class, true, PsiCodeBlock.class, PsiField.class);
        return annotation != null && SUPPRESS_INSPECTIONS_ANNOTATION_NAME.equals(annotation.getQualifiedName());
    }

    
    @Override
    public SuppressQuickFix[] createBatchSuppressActions( HighlightDisplayKey key) {
        return BatchSuppressManager.SERVICE.getInstance().createBatchSuppressActions(key);
    }

    
    public abstract SuppressIntentionAction[] createSuppressActions( HighlightDisplayKey key);
}