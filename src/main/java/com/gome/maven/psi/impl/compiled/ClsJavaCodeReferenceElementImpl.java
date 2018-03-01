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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiSubstitutorImpl;
import com.gome.maven.psi.impl.ResolveScopeManager;
import com.gome.maven.psi.impl.cache.TypeInfo;
import com.gome.maven.psi.impl.source.resolve.ResolveCache;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.HashMap;

import java.util.Map;

public class ClsJavaCodeReferenceElementImpl extends ClsElementImpl implements PsiJavaCodeReferenceElement {
    private final PsiElement myParent;
    private final String myCanonicalText;
    private final String myQualifiedName;
    private final PsiReferenceParameterList myRefParameterList;

    public ClsJavaCodeReferenceElementImpl(PsiElement parent,  String canonicalText) {
        myParent = parent;

        String canonical = TypeInfo.internFrequentType(canonicalText);
        myCanonicalText = canonical;
        String qName = TypeInfo.internFrequentType(PsiNameHelper.getQualifiedClassName(myCanonicalText, false));
        myQualifiedName = qName.equals(canonical) ? canonical : qName;

        String[] classParameters = PsiNameHelper.getClassParametersText(canonicalText);
        myRefParameterList = classParameters.length == 0 ? null : new ClsReferenceParameterListImpl(this, classParameters);
    }

    @Override
    
    public PsiElement[] getChildren() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public String getText() {
        return PsiNameHelper.getPresentableText(this);
    }

    @Override
    public int getTextLength() {
        return getText().length();
    }

    @Override
    public PsiReference getReference() {
        return this;
    }

    @Override
    
    public String getCanonicalText() {
        return myCanonicalText;
    }

    private static class Resolver implements ResolveCache.PolyVariantContextResolver<ClsJavaCodeReferenceElementImpl> {
        public static final Resolver INSTANCE = new Resolver();

        
        @Override
        public JavaResolveResult[] resolve( ClsJavaCodeReferenceElementImpl ref,  PsiFile containingFile, boolean incompleteCode) {
            final JavaResolveResult resolveResult = ref.advancedResolveImpl(containingFile);
            return resolveResult == null ? JavaResolveResult.EMPTY_ARRAY : new JavaResolveResult[] {resolveResult};
        }
    }

    private JavaResolveResult advancedResolveImpl( PsiFile containingFile) {
        PsiTypeElement[] typeElements = myRefParameterList == null ? PsiTypeElement.EMPTY_ARRAY : myRefParameterList.getTypeParameterElements();
        PsiElement resolve = resolveElement(containingFile);
        if (resolve == null) return null;
        if (resolve instanceof PsiClass) {
            Map<PsiTypeParameter, PsiType> substitutionMap = new HashMap<PsiTypeParameter, PsiType>();
            int index = 0;
            for (PsiTypeParameter parameter : PsiUtil.typeParametersIterable((PsiClass)resolve)) {
                if (index >= typeElements.length) {
                    PsiTypeParameterListOwner parameterOwner = parameter.getOwner();
                    if (parameterOwner == resolve) {
                        substitutionMap.put(parameter, null);
                    }
                    else if (parameterOwner instanceof PsiClass) {
                        PsiElement containingClass = myParent;
                        while ((containingClass = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class, true)) != null) {
                            PsiSubstitutor superClassSubstitutor =
                                    TypeConversionUtil.getClassSubstitutor((PsiClass)parameterOwner, (PsiClass)containingClass, PsiSubstitutor.EMPTY);
                            if (superClassSubstitutor != null) {
                                substitutionMap.put(parameter, superClassSubstitutor.substitute(parameter));
                                break;
                            }
                        }
                    }
                }
                else {
                    substitutionMap.put(parameter, typeElements[index].getType());
                }
                index++;
            }
            collectOuterClassTypeArgs((PsiClass)resolve, myCanonicalText, substitutionMap);
            return new CandidateInfo(resolve, PsiSubstitutorImpl.createSubstitutor(substitutionMap));
        }
        else {
            return new CandidateInfo(resolve, PsiSubstitutor.EMPTY);
        }
    }

    private void collectOuterClassTypeArgs( PsiClass psiClass,
                                           final String canonicalText,
                                           final Map<PsiTypeParameter, PsiType> substitutionMap) {
        final PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass != null && !containingClass.hasModifierProperty(PsiModifier.STATIC)) {
            final String outerClassRef = StringUtil.getPackageName(canonicalText);
            final String[] classParameters = PsiNameHelper.getClassParametersText(outerClassRef);
            final PsiType[] args = classParameters.length == 0 ? null : new ClsReferenceParameterListImpl(this, classParameters).getTypeArguments();
            final PsiTypeParameter[] typeParameters = containingClass.getTypeParameters();
            for (int i = 0; i < typeParameters.length; i++) {
                if (args != null && i < args.length) {
                    substitutionMap.put(typeParameters[i], args[i]);
                }
            }
            collectOuterClassTypeArgs(containingClass, outerClassRef, substitutionMap);
        }
    }

    @Override
    
    public JavaResolveResult advancedResolve(boolean incompleteCode) {
        final JavaResolveResult[] results = multiResolve(incompleteCode);
        if (results.length == 1) return results[0];
        return JavaResolveResult.EMPTY;
    }

    @Override
    
    public JavaResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile file = getContainingFile();
        final ResolveCache resolveCache = ResolveCache.getInstance(file.getProject());
        ResolveResult[] results = resolveCache.resolveWithCaching(this, Resolver.INSTANCE, true, incompleteCode,file);
        if (results.length == 0) return JavaResolveResult.EMPTY_ARRAY;
        return (JavaResolveResult[])results;
    }

    @Override
    public PsiElement resolve() {
        return advancedResolve(false).getElement();
    }

    
    private PsiElement resolveElement( PsiFile containingFile) {
        PsiElement element = getParent();
        while(element != null && (!(element instanceof PsiClass) || element instanceof PsiTypeParameter)) {
            if(element instanceof PsiMethod){
                final PsiMethod method = (PsiMethod)element;
                final PsiTypeParameterList list = method.getTypeParameterList();
                if (list != null) {
                    final PsiTypeParameter[] parameters = list.getTypeParameters();
                    for (int i = 0; parameters != null && i < parameters.length; i++) {
                        final PsiTypeParameter parameter = parameters[i];
                        if (myQualifiedName.equals(parameter.getName())) return parameter;
                    }
                }
            }
            element = element.getParent();
        }
        if (element == null) return null;
        for (PsiTypeParameter parameter : PsiUtil.typeParametersIterable((PsiTypeParameterListOwner)element)) {
            if (myQualifiedName.equals(parameter.getName())) return parameter;
        }
        return resolveClassPreferringMyJar(containingFile);
    }

    
    private PsiClass resolveClassPreferringMyJar( PsiFile containingFile) {
        Project project = containingFile.getProject();
        GlobalSearchScope scope = ResolveScopeManager.getInstance(project).getResolveScope(this);
        PsiClass[] classes = JavaPsiFacade.getInstance(project).findClasses(myQualifiedName, scope);
        if (classes.length == 0) return null;

        if (classes.length > 1) {
            VirtualFile jarFile = PsiUtil.getJarFile(containingFile);
            if (jarFile != null) {
                for (PsiClass aClass : classes) {
                    if (Comparing.equal(PsiUtil.getJarFile(aClass), jarFile)) return aClass;
                }
            }
        }
        return classes[0];
    }

    @Override
    public void processVariants( PsiScopeProcessor processor) {
        throw new RuntimeException("Variants are not available for light references");
    }

    @Override
    public PsiElement getReferenceNameElement() {
        return null;
    }

    @Override
    public PsiReferenceParameterList getParameterList() {
        return myRefParameterList;
    }

    @Override
    public String getQualifiedName() {
        return getCanonicalText();
    }

    @Override
    public String getReferenceName() {
        return PsiNameHelper.getShortClassName(myCanonicalText);
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public PsiElement bindToElement( PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        if (!(element instanceof PsiClass)) return false;
        PsiClass aClass = (PsiClass)element;
        return myCanonicalText.equals(aClass.getQualifiedName()) || getManager().areElementsEquivalent(resolve(), element);
    }

    @Override
    
    public Object[] getVariants() {
        throw new RuntimeException("Variants are not available for references to compiled code");
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Override
    public void appendMirrorText(final int indentLevel,  final StringBuilder buffer) {
        buffer.append(getCanonicalText());
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, JavaElementType.JAVA_CODE_REFERENCE);
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitReferenceElement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }


    public String toString() {
        return "PsiJavaCodeReferenceElement:" + getText();
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Override
    public PsiElement getElement() {
        return this;
    }

    @Override
    
    public PsiType[] getTypeParameters() {
        return myRefParameterList == null ? PsiType.EMPTY_ARRAY : myRefParameterList.getTypeArguments();
    }

    @Override
    public boolean isQualified() {
        return false;
    }

    @Override
    public PsiElement getQualifier() {
        return null;
    }
}
