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

public class ClassInitializerElement extends CompositeElement implements Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.ClassInitializerElement");

    public ClassInitializerElement() {
        super(CLASS_INITIALIZER);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.MODIFIER_LIST:
                return findChildByType(MODIFIER_LIST);

            case ChildRole.METHOD_BODY:
                return findChildByType(CODE_BLOCK);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == C_STYLE_COMMENT || i == END_OF_LINE_COMMENT) {
            {
                return ChildRoleBase.NONE;
            }
        }
        else if (i == MODIFIER_LIST) {
            return ChildRole.MODIFIER_LIST;
        }
        else if (i == CODE_BLOCK) {
            return ChildRole.METHOD_BODY;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }
}
