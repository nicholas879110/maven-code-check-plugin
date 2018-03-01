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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

/**
 * @author max
 */
public class TypeParameterElement extends CompositeElement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.TypeParameterElement");

    public TypeParameterElement() {
        super(JavaElementType.TYPE_PARAMETER);
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        final IElementType i = child.getElementType();
        if (i == JavaTokenType.IDENTIFIER) {
            return getChildRole(child, ChildRole.NAME);
        }
        else if (i == JavaElementType.EXTENDS_BOUND_LIST) {
            return getChildRole(child, ChildRole.EXTENDS_LIST);
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

            case ChildRole.NAME:
                return findChildByType(JavaTokenType.IDENTIFIER);

            case ChildRole.EXTENDS_LIST:
                return findChildByType(JavaElementType.EXTENDS_BOUND_LIST);
        }
    }
}
