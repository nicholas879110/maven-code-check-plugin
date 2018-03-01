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

package com.gome.maven.psi.filters;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.TokenType;
import com.gome.maven.psi.impl.source.tree.TreeUtil;

/**
 * @author Dmitry Avdeev
 */
public class FilterPositionUtil {

    
    public static PsiElement searchNonSpaceNonCommentBack(PsiElement element) {
        return searchNonSpaceNonCommentBack(element, false);
    }

    
    public static PsiElement searchNonSpaceNonCommentBack(PsiElement element, final boolean strict) {
        if(element == null || element.getNode() == null) return null;
        ASTNode leftNeibour = TreeUtil.prevLeaf(element.getNode());
        if (!strict) {
            while (leftNeibour != null && (leftNeibour.getElementType() == TokenType.WHITE_SPACE || leftNeibour.getPsi() instanceof PsiComment)){
                leftNeibour = TreeUtil.prevLeaf(leftNeibour);
            }
        }
        return leftNeibour != null ? leftNeibour.getPsi() : null;

    }
}
