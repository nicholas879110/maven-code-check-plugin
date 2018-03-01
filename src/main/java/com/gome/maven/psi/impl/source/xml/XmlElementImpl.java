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
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Aug 26, 2002
 * Time: 6:25:08 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.psi.impl.source.xml;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.TokenType;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.impl.source.tree.CompositePsiElement;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.PsiElementProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlElementType;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.xml.util.XmlPsiUtil;

public abstract class XmlElementImpl extends CompositePsiElement implements XmlElement {
    public XmlElementImpl(IElementType type) {
        super(type);
    }

    @Override
    public boolean processElements(PsiElementProcessor processor, PsiElement place){
        return XmlPsiUtil.processXmlElements(this, processor, false);
    }

    public boolean processChildren(PsiElementProcessor processor){
        return XmlPsiUtil.processXmlElementChildren(this, processor, false);
    }

    public XmlElement findElementByTokenType(final IElementType type){
        final XmlElement[] result = new XmlElement[1];
        result[0] = null;

        processElements(new PsiElementProcessor(){
            @Override
            public boolean execute( PsiElement element){
                if(element instanceof TreeElement && ((ASTNode)element).getElementType() == type){
                    result[0] = (XmlElement)element;
                    return false;
                }
                return true;
            }
        }, this);

        return result[0];
    }

    @Override
    public PsiElement getContext() {
        final XmlElement data = getUserData(INCLUDING_ELEMENT);
        if(data != null) return data;
        return getAstParent();
    }

    private PsiElement getAstParent() {
        return super.getParent();
    }

    @Override
    
    public PsiElement getNavigationElement() {
        if (!isPhysical()) {
            final XmlElement including = getUserData(INCLUDING_ELEMENT);
            if (including != null) {
                return including;
            }
            PsiElement astParent = getAstParent();
            PsiElement parentNavigation = astParent.getNavigationElement();
            if (parentNavigation.getTextOffset() == getTextOffset()) return parentNavigation;
            return this;
        }
        return super.getNavigationElement();
    }

    @Override
    public PsiElement getParent(){
        return getContext();
    }

    @Override
    
    public Language getLanguage() {
        return getContainingFile().getLanguage();
    }

    
    protected static String getNameFromEntityRef(final CompositeElement compositeElement, final IElementType xmlEntityDeclStart) {
        final ASTNode node = compositeElement.findChildByType(xmlEntityDeclStart);
        if (node == null) return null;
        ASTNode name = node.getTreeNext();

        if (name != null && name.getElementType() == TokenType.WHITE_SPACE) {
            name = name.getTreeNext();
        }

        if (name != null && name.getElementType() == XmlElementType.XML_ENTITY_REF) {
            final StringBuilder builder = new StringBuilder();

            ((XmlElement)name.getPsi()).processElements(new PsiElementProcessor() {
                @Override
                public boolean execute( final PsiElement element) {
                    builder.append(element.getText());
                    return true;
                }
            }, name.getPsi());
            if (builder.length() > 0) return builder.toString();
        }
        return null;
    }

    @Override
    
    public SearchScope getUseScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {

        if (super.isEquivalentTo(another)) return true;
        PsiElement element1 = this;

        // TODO: seem to be only necessary for tag dirs equivalents checking.
        if (element1 instanceof XmlTag && another instanceof XmlTag) {
            if (!element1.isPhysical() && !another.isPhysical()) return element1.getText().equals(another.getText());
        }

        return false;
    }
}
