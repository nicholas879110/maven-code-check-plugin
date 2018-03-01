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

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author dsl
 */
public class PsiImmediateClassType extends PsiClassType.Stub {
    private final PsiClass myClass;
    private final PsiSubstitutor mySubstitutor;
    private final PsiManager myManager;
    private String myCanonicalText;
    private String myCanonicalTextAnnotated;
    private String myPresentableText;
    private String myInternalCanonicalText;

    private final ClassResolveResult myClassResolveResult = new ClassResolveResult() {
        @Override
        public PsiClass getElement() {
            return myClass;
        }

        
        @Override
        public PsiSubstitutor getSubstitutor() {
            return mySubstitutor;
        }

        @Override
        public boolean isValidResult() {
            return true;
        }

        @Override
        public boolean isAccessible() {
            return true;
        }

        @Override
        public boolean isStaticsScopeCorrect() {
            return true;
        }

        @Override
        public PsiElement getCurrentFileResolveScope() {
            return null;
        }

        @Override
        public boolean isPackagePrefixPackageReference() {
            return false;
        }
    };

    public PsiImmediateClassType( PsiClass aClass,  PsiSubstitutor substitutor) {
        this(aClass, substitutor, null, PsiAnnotation.EMPTY_ARRAY);
    }

    public PsiImmediateClassType( PsiClass aClass,  PsiSubstitutor substitutor,  LanguageLevel level) {
        this(aClass, substitutor, level, PsiAnnotation.EMPTY_ARRAY);
    }

    public PsiImmediateClassType( PsiClass aClass,
                                  PsiSubstitutor substitutor,
                                  LanguageLevel level,
                                  PsiAnnotation... annotations) {
        super(level, annotations);
        myClass = aClass;
        myManager = aClass.getManager();
        mySubstitutor = substitutor;
        assert substitutor.isValid();
    }

    @Override
    public PsiClass resolve() {
        return myClass;
    }

    @Override
    public String getClassName() {
        return myClass.getName();
    }
    @Override
    
    public PsiType[] getParameters() {
        final PsiTypeParameter[] parameters = myClass.getTypeParameters();
        if (parameters.length == 0) {
            return PsiType.EMPTY_ARRAY;
        }

        List<PsiType> lst = new ArrayList<PsiType>();
        for (PsiTypeParameter parameter : parameters) {
            PsiType substituted = mySubstitutor.substitute(parameter);
            if (substituted != null) {
                lst.add(substituted);
            }
        }
        return lst.toArray(createArray(lst.size()));
    }

    @Override
    
    public ClassResolveResult resolveGenerics() {
        return myClassResolveResult;
    }

    @Override
    
    public PsiClassType rawType() {
        return JavaPsiFacade.getInstance(myClass.getProject()).getElementFactory().createType(myClass);
    }

    
    @Override
    public String getPresentableText() {
        if (myPresentableText == null) {
            myPresentableText = getText(TextType.PRESENTABLE, true);
        }
        return myPresentableText;
    }

    
    @Override
    public String getCanonicalText(boolean annotated) {
        String cached = annotated ? myCanonicalTextAnnotated : myCanonicalText;
        if (cached == null) {
            cached = getText(TextType.CANONICAL, annotated);
            if (annotated) myCanonicalTextAnnotated = cached;
            else myCanonicalText = cached;
        }
        return cached;
    }

    
    @Override
    public String getInternalCanonicalText() {
        if (myInternalCanonicalText == null) {
            myInternalCanonicalText = getText(TextType.INT_CANONICAL, true);
        }
        return myInternalCanonicalText;
    }

    private enum TextType { PRESENTABLE, CANONICAL, INT_CANONICAL }

    private String getText( TextType textType, boolean annotated) {
        mySubstitutor.ensureValid();
        StringBuilder buffer = new StringBuilder();
        buildText(myClass, mySubstitutor, buffer, textType, annotated);
        return buffer.toString();
    }

    private void buildText( PsiClass aClass,
                            PsiSubstitutor substitutor,
                            StringBuilder buffer,
                            TextType textType,
                           boolean annotated) {
        if (aClass instanceof PsiAnonymousClass) {
            ClassResolveResult baseResolveResult = ((PsiAnonymousClass)aClass).getBaseClassType().resolveGenerics();
            PsiClass baseClass = baseResolveResult.getElement();
            if (baseClass != null) {
                if (textType == TextType.INT_CANONICAL) {
                    buffer.append("anonymous ");
                }
                buildText(baseClass, baseResolveResult.getSubstitutor(), buffer, textType, false);
            }
            return;
        }

        boolean qualified = textType != TextType.PRESENTABLE;

        PsiClass enclosingClass = null;
        if (!aClass.hasModifierProperty(PsiModifier.STATIC)) {
            PsiElement parent = aClass.getParent();
            if (parent instanceof PsiClass && !(parent instanceof PsiAnonymousClass)) {
                enclosingClass = (PsiClass)parent;
            }
        }
        if (enclosingClass != null) {
            buildText(enclosingClass, substitutor, buffer, textType, false);
            buffer.append('.');
        }
        else if (qualified) {
            String fqn = aClass.getQualifiedName();
            if (fqn != null) {
                String prefix = StringUtil.getPackageName(fqn);
                if (!StringUtil.isEmpty(prefix)) {
                    buffer.append(prefix);
                    buffer.append('.');
                }
            }
        }

        if (annotated) {
            PsiNameHelper.appendAnnotations(buffer, getAnnotations(), qualified);
        }

        buffer.append(aClass.getName());

        PsiTypeParameter[] typeParameters = aClass.getTypeParameters();
        if (typeParameters.length > 0) {
            int pos = buffer.length();
            buffer.append('<');

            for (int i = 0; i < typeParameters.length; i++) {
                PsiTypeParameter typeParameter = typeParameters[i];
                PsiUtilCore.ensureValid(typeParameter);

                if (i > 0) {
                    buffer.append(',');
                    if (textType == TextType.PRESENTABLE) buffer.append(' ');
                }

                PsiType substitutionResult = substitutor.substitute(typeParameter);
                if (substitutionResult == null) {
                    buffer.setLength(pos);
                    pos = -1;
                    break;
                }
                PsiUtil.ensureValidType(substitutionResult);

                if (textType == TextType.PRESENTABLE) {
                    buffer.append(substitutionResult.getPresentableText());
                }
                else if (textType == TextType.CANONICAL) {
                    buffer.append(substitutionResult.getCanonicalText(annotated));
                }
                else {
                    buffer.append(substitutionResult.getInternalCanonicalText());
                }
            }

            if (pos >= 0) {
                buffer.append('>');
            }
        }
    }

    @Override
    public boolean isValid() {
        return myClass.isValid() && mySubstitutor.isValid();
    }

    @Override
    public boolean equalsToText( String text) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(myManager.getProject()).getElementFactory();
        final PsiType patternType;
        try {
            patternType = factory.createTypeFromText(text, myClass);
        }
        catch (IncorrectOperationException e) {
            return false;
        }
        return equals(patternType);
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        return myClass.getResolveScope();
    }

    @Override
    
    public LanguageLevel getLanguageLevel() {
        return myLanguageLevel != null ? myLanguageLevel : PsiUtil.getLanguageLevel(myClass);
    }

    
    @Override
    public PsiClassType setLanguageLevel( LanguageLevel level) {
        return level.equals(myLanguageLevel) ? this : new PsiImmediateClassType(myClass, mySubstitutor, level, getAnnotations());
    }
}
