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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.TreeElement;

public class ClsReferenceParameterListImpl extends ClsElementImpl implements PsiReferenceParameterList {
     private static final String EXTENDS_PREFIX = "?extends";
     private static final String SUPER_PREFIX = "?super";

    private final PsiElement myParent;
    private final ClsTypeElementImpl[] myTypeParameters;
    private volatile PsiType[] myTypeParametersCachedTypes = null;

    public ClsReferenceParameterListImpl(PsiElement parent, String[] classParameters) {
        myParent = parent;

        int length = classParameters.length;
        myTypeParameters = new ClsTypeElementImpl[length];

        for (int i = 0; i < length; i++) {
            String s = classParameters[length - i - 1];
            char variance = ClsTypeElementImpl.VARIANCE_NONE;
            if (s.startsWith(EXTENDS_PREFIX)) {
                variance = ClsTypeElementImpl.VARIANCE_EXTENDS;
                s = s.substring(EXTENDS_PREFIX.length());
            }
            else if (s.startsWith(SUPER_PREFIX)) {
                variance = ClsTypeElementImpl.VARIANCE_SUPER;
                s = s.substring(SUPER_PREFIX.length());
            }
            else if (StringUtil.startsWithChar(s, '?')) {
                variance = ClsTypeElementImpl.VARIANCE_INVARIANT;
                s = s.substring(1);
            }

            myTypeParameters[i] = new ClsTypeElementImpl(this, s, variance);
        }
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) { }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException { }

    
    @Override
    public PsiTypeElement[] getTypeParameterElements() {
        return myTypeParameters;
    }

    
    @Override
    public PsiType[] getTypeArguments() {
        PsiType[] cachedTypes = myTypeParametersCachedTypes;
        if (cachedTypes == null) {
            cachedTypes = PsiType.createArray(myTypeParameters.length);
            for (int i = 0; i < cachedTypes.length; i++) {
                cachedTypes[cachedTypes.length - i - 1] = myTypeParameters[i].getType();
            }
            myTypeParametersCachedTypes = cachedTypes;
        }
        return cachedTypes;
    }

    
    @Override
    public PsiElement[] getChildren() {
        return myTypeParameters;
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }
}
