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
package com.gome.maven.psi.impl.source;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.ItemPresentationProviders;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.ui.Queryable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.*;
import com.gome.maven.psi.impl.cache.TypeInfo;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiFieldStub;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.impl.source.resolve.JavaResolveCache;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.stubs.IStubElementType;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PsiFieldImpl extends JavaStubPsiElement<PsiFieldStub> implements PsiField, PsiVariableEx, Queryable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiFieldImpl");

    private volatile SoftReference<PsiType> myCachedType = null;
    private volatile Object myCachedInitializerValue = null; // PsiExpression on constant value for literal

    public PsiFieldImpl(final PsiFieldStub stub) {
        this(stub, JavaStubElementTypes.FIELD);
    }

    protected PsiFieldImpl(final PsiFieldStub stub, final IStubElementType type) {
        super(stub, type);
    }

    public PsiFieldImpl(final ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        dropCached();
    }

    private void dropCached() {
        myCachedInitializerValue = null;
        myCachedType = null;
    }

    @Override
    protected Object clone() {
        PsiFieldImpl clone = (PsiFieldImpl)super.clone();
        clone.dropCached();
        return clone;
    }

    @Override
    public PsiClass getContainingClass() {
        PsiElement parent = getParent();
        return parent instanceof PsiClass ? (PsiClass)parent : PsiTreeUtil.getParentOfType(this, PsiSyntheticClass.class);
    }

    @Override
    public PsiElement getContext() {
        final PsiClass cc = getContainingClass();
        return cc != null ? cc : super.getContext();
    }

    @Override
    
    public CompositeElement getNode() {
        return (CompositeElement)super.getNode();
    }

    @Override
    
    public PsiIdentifier getNameIdentifier() {
        return (PsiIdentifier)getNode().findChildByRoleAsPsiElement(ChildRole.NAME);
    }

    @Override
    
    public String getName() {
        final PsiFieldStub stub = getStub();
        if (stub != null) {
            return stub.getName();
        }
        return getNameIdentifier().getText();
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException{
        PsiImplUtil.setName(getNameIdentifier(), name);
        return this;
    }

    @Override
    
    public PsiType getType() {
        final PsiFieldStub stub = getStub();
        if (stub != null) {
            PsiType type = SoftReference.dereference(myCachedType);
            if (type != null) return type;

            String typeText = TypeInfo.createTypeText(stub.getType(true));
            try {
                type = JavaPsiFacade.getInstance(getProject()).getParserFacade().createTypeFromText(typeText, this);
                myCachedType = new SoftReference<PsiType>(type);
                return type;
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
                return null;
            }
        }

        myCachedType = null;
        return JavaSharedImplUtil.getType(getTypeElement(), getNameIdentifier());
    }

    @Override
    public PsiTypeElement getTypeElement(){
        PsiField firstField = findFirstFieldInDeclaration();
        if (firstField != this){
            return firstField.getTypeElement();
        }

        return (PsiTypeElement)getNode().findChildByRoleAsPsiElement(ChildRole.TYPE);
    }

    @Override
    
    public PsiModifierList getModifierList() {
        final PsiModifierList selfModifierList = getSelfModifierList();
        if (selfModifierList != null) {
            return selfModifierList;
        }
        PsiField firstField = findFirstFieldInDeclaration();
        if (firstField == this) {
            if (!isValid()) throw new PsiInvalidElementAccessException(this);

            final PsiField lastResort = findFirstFieldByTree();
            if (lastResort == this) {
                throw new IllegalStateException("Missing modifier list for sequence of fields: '" + getText() + "'");
            }

            firstField = lastResort;
        }

        return firstField.getModifierList();
    }

    
    private PsiModifierList getSelfModifierList() {
        return getStubOrPsiChild(JavaStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifierProperty( String name) {
        return getModifierList().hasModifierProperty(name);
    }

    private PsiField findFirstFieldInDeclaration() {
        if (getSelfModifierList() != null) return this;

        final PsiFieldStub stub = getStub();
        if (stub != null) {
            final List siblings = stub.getParentStub().getChildrenStubs();
            final int idx = siblings.indexOf(stub);
            assert idx >= 0;
            for (int i = idx - 1; i >= 0; i--) {
                if (!(siblings.get(i) instanceof PsiFieldStub)) break;
                PsiFieldStub prevField = (PsiFieldStub)siblings.get(i);
                final PsiFieldImpl prevFieldPsi = (PsiFieldImpl)prevField.getPsi();
                if (prevFieldPsi.getSelfModifierList() != null) return prevFieldPsi;
            }
        }

        return findFirstFieldByTree();
    }

    private PsiField findFirstFieldByTree() {
        CompositeElement treeElement = getNode();

        ASTNode modifierList = treeElement.findChildByRole(ChildRole.MODIFIER_LIST);
        if (modifierList == null) {
            ASTNode prevField = treeElement.getTreePrev();
            while (prevField != null && prevField.getElementType() != JavaElementType.FIELD) {
                prevField = prevField.getTreePrev();
            }
            if (prevField == null) return this;
            return ((PsiFieldImpl)SourceTreeToPsiMap.treeElementToPsi(prevField)).findFirstFieldInDeclaration();
        }
        else {
            return this;
        }
    }

    @Override
    public PsiExpression getInitializer() {
        return (PsiExpression)getNode().findChildByRoleAsPsiElement(ChildRole.INITIALIZER);
    }

    @Override
    public boolean hasInitializer() {
        PsiFieldStub stub = getStub();
        if (stub != null) {
            return stub.getInitializerText() != null;
        }

        return getInitializer() != null;
    }

    @Override
    public Icon getElementIcon(final int flags) {
        final RowIcon baseIcon = ElementPresentationUtil.createLayeredIcon(PlatformIcons.FIELD_ICON, this, false);
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
    }

    private static class OurConstValueComputer implements JavaResolveCache.ConstValueComputer {
        private static final OurConstValueComputer INSTANCE = new OurConstValueComputer();

        @Override
        public Object execute( PsiVariable variable, Set<PsiVariable> visitedVars) {
            return ((PsiFieldImpl)variable)._computeConstantValue(visitedVars);
        }
    }

    
    private Object _computeConstantValue(Set<PsiVariable> visitedVars) {
        Object cachedInitializerValue = myCachedInitializerValue;
        if (cachedInitializerValue != null && !(cachedInitializerValue instanceof PsiExpression)){
            return cachedInitializerValue;
        }

        PsiType type = getType();
        // javac rejects all non primitive and non String constants, although JLS states constants "variables whose initializers are constant expressions"
        if (!(type instanceof PsiPrimitiveType) && !type.equalsToText("java.lang.String")) return null;

        PsiExpression initializer;
        if (cachedInitializerValue != null) {
            initializer = (PsiExpression)cachedInitializerValue;
        }
        else{
            final PsiFieldStub stub = getStub();
            if (stub == null) {
                initializer = getInitializer();
                if (initializer == null) return null;
            }
            else{
                String initializerText = stub.getInitializerText();
                if (StringUtil.isEmpty(initializerText)) return null;

                if (PsiFieldStub.INITIALIZER_NOT_STORED.equals(initializerText)) return null;
                if (PsiFieldStub.INITIALIZER_TOO_LONG.equals(initializerText)) {
                    getNode();
                    return computeConstantValue(visitedVars);
                }

                final PsiJavaParserFacade parserFacade = JavaPsiFacade.getInstance(getProject()).getParserFacade();
                initializer = parserFacade.createExpressionFromText(initializerText, this);
            }
        }

        Object result = PsiConstantEvaluationHelperImpl.computeCastTo(initializer, type, visitedVars);

        if (initializer instanceof PsiLiteralExpression){
            myCachedInitializerValue = result;
        }
        else{
            myCachedInitializerValue = initializer;
        }


        return result;
    }

    @Override
    public Object computeConstantValue() {
        Object cachedInitializerValue = myCachedInitializerValue;
        if (cachedInitializerValue != null && !(cachedInitializerValue instanceof PsiExpression)){
            return cachedInitializerValue;
        }

        return computeConstantValue(new HashSet<PsiVariable>(2));
    }

    @Override
    public Object computeConstantValue(Set<PsiVariable> visitedVars) {
        if (!hasModifierProperty(PsiModifier.FINAL)) return null;

        return JavaResolveCache.getInstance(getProject()).computeConstantValueWithCaching(this, OurConstValueComputer.INSTANCE, visitedVars);
    }

    @Override
    public boolean isDeprecated() {
        final PsiFieldStub stub = getStub();
        if (stub != null) {
            return stub.isDeprecated() || stub.hasDeprecatedAnnotation() && PsiImplUtil.isDeprecatedByAnnotation(this);
        }

        return PsiImplUtil.isDeprecatedByDocTag(this) || PsiImplUtil.isDeprecatedByAnnotation(this);
    }

    @Override
    public PsiDocComment getDocComment(){
        CompositeElement treeElement = getNode();
        if (getTypeElement() != null) {
            PsiElement element = treeElement.findChildByRoleAsPsiElement(ChildRole.DOC_COMMENT);
            return element instanceof PsiDocComment ? (PsiDocComment)element : null;
        }
        else {
            ASTNode prevField = treeElement.getTreePrev();
            while(prevField.getElementType() != JavaElementType.FIELD){
                prevField = prevField.getTreePrev();
            }
            return ((PsiField)SourceTreeToPsiMap.treeElementToPsi(prevField)).getDocComment();
        }
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException{
        CheckUtil.checkWritable(this);

        final PsiTypeElement type = getTypeElement();
        PsiElement modifierList = getModifierList();
        ASTNode field = SourceTreeToPsiMap.psiElementToTree(type.getParent());
        while(true){
            ASTNode comma = PsiImplUtil.skipWhitespaceAndComments(field.getTreeNext());
            if (comma == null || comma.getElementType() != JavaTokenType.COMMA) break;
            ASTNode nextField = PsiImplUtil.skipWhitespaceAndComments(comma.getTreeNext());
            if (nextField == null || nextField.getElementType() != JavaElementType.FIELD) break;

            TreeElement semicolon = Factory.createSingleLeafElement(JavaTokenType.SEMICOLON, ";", 0, 1, null, getManager());
            CodeEditUtil.addChild(field, semicolon, null);

            CodeEditUtil.removeChild(comma.getTreeParent(), comma);

            PsiElement typeClone = type.copy();
            CodeEditUtil.addChild(nextField, SourceTreeToPsiMap.psiElementToTree(typeClone), nextField.getFirstChildNode());

            PsiElement modifierListClone = modifierList.copy();
            CodeEditUtil.addChild(nextField, SourceTreeToPsiMap.psiElementToTree(modifierListClone), nextField.getFirstChildNode());

            field = nextField;
        }

        JavaSharedImplUtil.normalizeBrackets(this);
    }

    @Override
    public void accept( PsiElementVisitor visitor){
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitField(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent,  PsiElement place){
        processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
        return true;
    }

    public String toString(){
        return "PsiField:" + getName();
    }

    @Override
    public PsiElement getOriginalElement() {
        PsiClass containingClass = getContainingClass();
        if (containingClass != null) {
            PsiField originalField = ((PsiClass)containingClass.getOriginalElement()).findFieldByName(getName(), false);
            if (originalField != null) {
                return originalField;
            }
        }
        return this;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public void setInitializer(PsiExpression initializer) throws IncorrectOperationException {
        JavaSharedImplUtil.setInitializer(this, initializer);
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return PsiClassImplUtil.isFieldEquivalentTo(this, another);
    }

    @Override
    
    public SearchScope getUseScope() {
        return PsiImplUtil.getMemberUseScope(this);
    }

    @Override
    public void putInfo( Map<String, String> info) {
        info.put("fieldName", getName());
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }

}
