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
package com.gome.maven.lang;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.psi.TokenType;
import com.gome.maven.psi.impl.source.CharTableImpl;
import com.gome.maven.psi.impl.source.CodeFragmentElement;
import com.gome.maven.psi.impl.source.DummyHolderElement;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.tree.*;
import com.gome.maven.util.CharTable;

/**
 * @author max
 */
public abstract class ASTFactory {
    private static final CharTable WHITESPACES = new CharTableImpl();

    // interface methods

    
    public LazyParseableElement createLazy(final ILazyParseableElementType type, final CharSequence text) {
        return null;
    }

    
    public CompositeElement createComposite(final IElementType type) {
        return null;
    }

    
    public LeafElement createLeaf( IElementType type, final CharSequence text) {
        return null;
    }

    // factory methods

    
    public static LazyParseableElement lazy( final ILazyParseableElementType type, final CharSequence text) {
        final ASTNode node = type.createNode(text);
        if (node != null) return (LazyParseableElement)node;

        if (type == TokenType.CODE_FRAGMENT) {
            return new CodeFragmentElement(null);
        }
        else if (type == TokenType.DUMMY_HOLDER) {
            return new DummyHolderElement(text);
        }

        final LazyParseableElement customLazy = factory(type).createLazy(type, text);
        return customLazy != null ? customLazy : DefaultFactoryHolder.DEFAULT.createLazy(type, text);
    }

    
    public static CompositeElement composite( final IElementType type) {
        if (type instanceof ICompositeElementType) {
            return (CompositeElement)((ICompositeElementType)type).createCompositeNode();
        }

        final CompositeElement customComposite = factory(type).createComposite(type);
        return customComposite != null ? customComposite : DefaultFactoryHolder.DEFAULT.createComposite(type);
    }

    
    public static LeafElement leaf( final IElementType type, final CharSequence text) {
        if (type == TokenType.WHITE_SPACE) {
            return new PsiWhiteSpaceImpl(text);
        }

        if (type instanceof ILeafElementType) {
            return (LeafElement)((ILeafElementType)type).createLeafNode(text);
        }

        final LeafElement customLeaf = factory(type).createLeaf(type, text);
        return customLeaf != null ? customLeaf : DefaultFactoryHolder.DEFAULT.createLeaf(type, text);
    }

    
    private static ASTFactory factory(final IElementType type) {
        return LanguageASTFactory.INSTANCE.forLanguage(type.getLanguage());
    }

    
    public static LeafElement whitespace(final CharSequence text) {
        final PsiWhiteSpaceImpl w = new PsiWhiteSpaceImpl(WHITESPACES.intern(text));
        CodeEditUtil.setNodeGenerated(w, true);
        return w;
    }

    public static class DefaultFactoryHolder {
        public static final ASTFactory DEFAULT = def();

        private static ASTFactory def() {
            return (ASTFactory)ServiceManager.getService(DefaultASTFactory.class);
        }

        private DefaultFactoryHolder() {
        }
    }
}
