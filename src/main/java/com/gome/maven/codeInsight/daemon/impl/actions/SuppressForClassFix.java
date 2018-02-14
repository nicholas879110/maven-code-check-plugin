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
package com.gome.maven.codeInsight.daemon.impl.actions;

import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiDeclarationStatement;
import com.gome.maven.psi.PsiDocCommentOwner;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiTreeUtil;

/**
 * User: anna
 * Date: May 13, 2005
 */
public class SuppressForClassFix extends SuppressFix {
    public SuppressForClassFix(final HighlightDisplayKey key) {
        super(key);
    }

    public SuppressForClassFix(final String id) {
        super(id);
    }

    @Override
    
    public PsiDocCommentOwner getContainer(final PsiElement element) {
        PsiDocCommentOwner container = super.getContainer(element);
        if (container == null || container instanceof PsiClass){
            return null;
        }
        while (container != null ) {
            final PsiClass parentClass = PsiTreeUtil.getParentOfType(container, PsiClass.class);
            if ((parentClass == null || container.getParent() instanceof PsiDeclarationStatement || container.getParent() instanceof PsiClass) && container instanceof PsiClass){
                return container;
            }
            container = parentClass;
        }
        return container;
    }

    @Override

    public String getText() {
        return InspectionsBundle.message("suppress.inspection.class");
    }
}
