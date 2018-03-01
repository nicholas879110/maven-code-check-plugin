/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.source.javadoc;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.javadoc.PsiDocTag;
import com.gome.maven.psi.javadoc.PsiDocTagValue;
import com.gome.maven.psi.javadoc.PsiDocToken;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.CharTable;
import com.gome.maven.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mike
 */
public class PsiDocParamRef extends CompositePsiElement implements PsiDocTagValue {
    public PsiDocParamRef() {
        super(JavaDocElementType.DOC_PARAMETER_REF);
    }

    @Override
    public PsiReference getReference() {
        final PsiDocComment comment = PsiTreeUtil.getParentOfType(this, PsiDocComment.class);
        if (comment == null) return null;
        final PsiDocCommentOwner owner = comment.getOwner();
        if (!(owner instanceof PsiMethod) &&
                !(owner instanceof PsiClass)) return null;
        final ASTNode valueToken = findChildByType(JavaDocTokenType.DOC_TAG_VALUE_TOKEN);
        if (valueToken == null) return null;
        final String name = valueToken.getText();
        PsiElement reference = null;
        final PsiElement firstChild = getFirstChild();
        if (firstChild instanceof PsiDocToken && ((PsiDocToken)firstChild).getTokenType().equals(JavaDocTokenType.DOC_TAG_VALUE_LT)) {
            final PsiTypeParameter[] typeParameters = ((PsiTypeParameterListOwner)owner).getTypeParameters();
            for (PsiTypeParameter typeParameter : typeParameters) {
                if (typeParameter.getName().equals(name)) {
                    reference = typeParameter;
                }
            }
        }
        else if (owner instanceof PsiMethod) {
            final PsiParameter[] parameters = ((PsiMethod)owner).getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                if (parameter.getName().equals(name)) {
                    reference = parameter;
                }
            }
        }

        final PsiElement resultReference = reference;
        return new PsiJavaReference() {
            @Override
            public PsiElement resolve() {
                return resultReference;
            }

            @Override
            
            public String getCanonicalText() {
                return valueToken.getText();
            }

            @Override
            public PsiElement handleElementRename(String newElementName) {
                final CharTable charTableByTree = SharedImplUtil.findCharTableByTree(getNode());
                LeafElement newElement = Factory.createSingleLeafElement(JavaDocTokenType.DOC_TAG_VALUE_TOKEN, newElementName, charTableByTree, getManager());
                replaceChild(valueToken, newElement);
                return PsiDocParamRef.this;
            }

            @Override
            public PsiElement bindToElement( PsiElement element) throws IncorrectOperationException {
                if (isReferenceTo(element)) return PsiDocParamRef.this;
                if(!(element instanceof PsiParameter)) {
                    throw new IncorrectOperationException("Unsupported operation");
                }
                return handleElementRename(((PsiParameter) element).getName());
            }

            @Override
            public boolean isReferenceTo(PsiElement element) {
                if (!(element instanceof PsiNamedElement)) return false;
                PsiNamedElement namedElement = (PsiNamedElement)element;
                if (!getCanonicalText().equals(namedElement.getName())) return false;
                return getManager().areElementsEquivalent(resolve(), element);
            }

            @Override
            
            public PsiElement[] getVariants() {
                final PsiElement firstChild = getFirstChild();

                Set<String> usedNames = new HashSet<String>();
                for (PsiDocTag tag : comment.getTags()) {
                    if (tag.getName().equals("param")) {
                        PsiDocTagValue valueElement = tag.getValueElement();
                        if (valueElement != null) {
                            usedNames.add(valueElement.getText());
                        }
                    }
                }

                PsiNamedElement[] result = PsiNamedElement.EMPTY_ARRAY;
                if (firstChild instanceof PsiDocToken && ((PsiDocToken)firstChild).getTokenType().equals(JavaDocTokenType.DOC_TAG_VALUE_LT)) {
                    result = ((PsiTypeParameterListOwner)owner).getTypeParameters();
                } else if (owner instanceof PsiMethod) {
                    result = ((PsiMethod)owner).getParameterList().getParameters();
                }
                List<PsiElement> filtered = new ArrayList<PsiElement>();
                for (PsiNamedElement namedElement : result) {
                    if (!usedNames.contains(namedElement.getName())) {
                        filtered.add(namedElement);
                    }
                }
                return filtered.toArray(new PsiElement[filtered.size()]);
            }

            @Override
            public boolean isSoft(){
                return false;
            }

            @Override
            public TextRange getRangeInElement() {
                final int startOffsetInParent = valueToken.getPsi().getStartOffsetInParent();
                return new TextRange(startOffsetInParent, startOffsetInParent + valueToken.getTextLength());
            }

            @Override
            public PsiElement getElement() {
                return PsiDocParamRef.this;
            }

            @Override
            public void processVariants( PsiScopeProcessor processor) {
                for (final PsiElement element : getVariants()) {
                    if (!processor.execute(element, ResolveState.initial())) {
                        return;
                    }
                }
            }

            @Override
            
            public JavaResolveResult advancedResolve(boolean incompleteCode) {
                return resultReference == null ? JavaResolveResult.EMPTY : new CandidateInfo(resultReference, PsiSubstitutor.EMPTY);
            }

            @Override
            
            public JavaResolveResult[] multiResolve(boolean incompleteCode) {
                return resultReference == null
                        ? JavaResolveResult.EMPTY_ARRAY
                        : new JavaResolveResult[]{new CandidateInfo(resultReference, PsiSubstitutor.EMPTY)};
            }
        };
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitDocTagValue(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
