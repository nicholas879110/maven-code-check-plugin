/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import com.gome.maven.psi.PsiJavaFile;
import com.gome.maven.psi.TokenType;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

public class JavaFileElement extends FileElement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.JavaFileElement");

    public JavaFileElement(CharSequence text) {
        super(JavaStubElementTypes.JAVA_FILE, text);
    }

    @Override
    public void deleteChildInternal( ASTNode child) {
        if (child.getElementType() == JavaElementType.CLASS) {
            PsiJavaFile file = SourceTreeToPsiMap.treeToPsiNotNull(this);
            if (file.getClasses().length == 1) {
                file.delete();
                return;
            }
        }
        super.deleteChildInternal(child);
    }

    @Override
    
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch (role) {
            default:
                return null;

            case ChildRole.PACKAGE_STATEMENT:
                return findChildByType(JavaElementType.PACKAGE_STATEMENT);

            case ChildRole.IMPORT_LIST:
                return findChildByType(JavaElementType.IMPORT_LIST);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == JavaElementType.PACKAGE_STATEMENT) {
            return ChildRole.PACKAGE_STATEMENT;
        }
        else if (i == JavaElementType.IMPORT_LIST) {
            return ChildRole.IMPORT_LIST;
        }
        else if (i == JavaElementType.CLASS) {
            return ChildRole.CLASS;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void replaceChildInternal( ASTNode child,  TreeElement newElement) {
        if (newElement.getElementType() == JavaElementType.IMPORT_LIST) {
            LOG.assertTrue(child.getElementType() == JavaElementType.IMPORT_LIST);
            if (newElement.getFirstChildNode() == null) { //empty import list
                ASTNode next = child.getTreeNext();
                if (next != null && next.getElementType() == TokenType.WHITE_SPACE) {
                    removeChild(next);
                }
            }
        }
        super.replaceChildInternal(child, newElement);
    }
}
