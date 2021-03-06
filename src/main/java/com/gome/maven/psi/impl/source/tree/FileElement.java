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

package com.gome.maven.psi.impl.source.tree;

import com.gome.maven.lang.*;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.source.CharTableImpl;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IFileElementType;
import com.gome.maven.psi.tree.ILightStubFileElementType;
import com.gome.maven.util.CharTable;

public class FileElement extends LazyParseableElement implements FileASTNode, Getter<FileElement> {
    private volatile CharTable myCharTable = new CharTableImpl();
    private volatile boolean myDetached;

    @Override
    protected PsiElement createPsiNoLock() {
        return myDetached ? null : super.createPsiNoLock();
    }

    public void detachFromFile() {
        myDetached = true;
        clearPsi();
    }

    @Override
    
    public CharTable getCharTable() {
        return myCharTable;
    }

    
    @Override
    public LighterAST getLighterAST() {
        final IFileElementType contentType = (IFileElementType)getElementType();
        assert contentType instanceof ILightStubFileElementType:contentType;

        LighterAST tree;
        if (!isParsed()) {
            return new FCTSBackedLighterAST(getCharTable(), ((ILightStubFileElementType<?>)contentType).parseContentsLight(this));
        }
        else {
            tree = new TreeBackedLighterAST(this);
        }
        return tree;
    }

    public FileElement(IElementType type, CharSequence text) {
        super(type, text);
    }

    @Deprecated  // for 8.1 API compatibility
    public FileElement(IElementType type) {
        super(type, null);
    }

    @Override
    public PsiManagerEx getManager() {
        CompositeElement treeParent = getTreeParent();
        if (treeParent != null) return treeParent.getManager();
        return (PsiManagerEx)getPsi().getManager(); //TODO: cache?
    }

    @Override
    public ASTNode copyElement() {
        PsiFileImpl psiElement = (PsiFileImpl)getPsi();
        PsiFileImpl psiElementCopy = (PsiFileImpl)psiElement.copy();
        return psiElementCopy.getTreeElement();
    }

    public void setCharTable( CharTable table) {
        myCharTable = table;
    }

    @Override
    public FileElement get() {
        return this;
    }
}
