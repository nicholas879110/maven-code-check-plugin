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

package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

/**
 * @author ven
 */
public class AnnotationElement extends CompositeElement implements Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.AnnotationElement");

    public AnnotationElement() {
        super(ANNOTATION);
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);

        IElementType i = child.getElementType();
        if (i == ANNOTATION_PARAMETER_LIST) {
            return ChildRole.PARAMETER_LIST;
        }
        else if (i == JAVA_CODE_REFERENCE) {
            return ChildRole.CLASS_REFERENCE;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch (role) {
            default:
                return null;

            case ChildRole.PARAMETER_LIST:
                return findChildByType(ANNOTATION_PARAMETER_LIST);

            case ChildRole.CLASS_REFERENCE:
                return findChildByType(JAVA_CODE_REFERENCE);
        }
    }
}
