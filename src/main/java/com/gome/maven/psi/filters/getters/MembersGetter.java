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
package com.gome.maven.psi.filters.getters;

import com.gome.maven.codeInsight.CodeInsightUtil;
import com.gome.maven.codeInsight.completion.CompletionUtil;
import com.gome.maven.codeInsight.completion.JavaCompletionUtil;
import com.gome.maven.codeInsight.completion.PrefixMatcher;
import com.gome.maven.codeInsight.completion.StaticMemberProcessor;
import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.*;
import com.gome.maven.psi.filters.TrueFilter;
import com.gome.maven.psi.impl.java.stubs.index.JavaStaticMemberTypeIndex;
import com.gome.maven.psi.scope.processor.FilterScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.util.InheritanceUtil;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.PairConsumer;

import java.util.*;

/**
 * @author ik
 * @author peter
 */
public abstract class MembersGetter {
    public static final Key<Boolean> EXPECTED_TYPE_MEMBER = Key.create("EXPECTED_TYPE_MEMBER");
    private final Set<PsiMember> myImportedStatically = new HashSet<PsiMember>();
    private final List<PsiClass> myPlaceClasses = new ArrayList<PsiClass>();
    private final List<PsiMethod> myPlaceMethods = new ArrayList<PsiMethod>();
    protected final PsiElement myPlace;

    protected MembersGetter(StaticMemberProcessor processor,  final PsiElement place) {
        myPlace = place;
        processor.processMembersOfRegisteredClasses(PrefixMatcher.ALWAYS_TRUE, new PairConsumer<PsiMember, PsiClass>() {
            @Override
            public void consume(PsiMember member, PsiClass psiClass) {
                myImportedStatically.add(member);
            }
        });

        PsiClass current = PsiTreeUtil.getContextOfType(place, PsiClass.class);
        while (current != null) {
            current = CompletionUtil.getOriginalOrSelf(current);
            myPlaceClasses.add(current);
            current = PsiTreeUtil.getContextOfType(current, PsiClass.class);
        }

        PsiMethod eachMethod = PsiTreeUtil.getContextOfType(place, PsiMethod.class);
        while (eachMethod != null) {
            eachMethod = CompletionUtil.getOriginalOrSelf(eachMethod);
            myPlaceMethods.add(eachMethod);
            eachMethod = PsiTreeUtil.getContextOfType(eachMethod, PsiMethod.class);
        }

    }

    private boolean mayProcessMembers( PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        for (PsiClass placeClass : myPlaceClasses) {
            if (InheritanceUtil.isInheritorOrSelf(placeClass, psiClass, true)) {
                return false;
            }
        }
        return true;
    }

    public void processMembers(final Consumer<LookupElement> results,  final PsiClass where,
                               final boolean acceptMethods, final boolean searchInheritors) {
        if (where == null || isPrimitiveClass(where)) return;

        final boolean searchFactoryMethods = searchInheritors &&
                !CommonClassNames.JAVA_LANG_OBJECT.equals(where.getQualifiedName()) &&
                !isPrimitiveClass(where);

        final Project project = myPlace.getProject();
        final GlobalSearchScope scope = myPlace.getResolveScope();

        final PsiClassType baseType = JavaPsiFacade.getElementFactory(project).createType(where);
        Consumer<PsiType> consumer = new Consumer<PsiType>() {
            @Override
            public void consume(PsiType psiType) {
                PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
                if (psiClass == null) {
                    return;
                }
                psiClass = CompletionUtil.getOriginalOrSelf(psiClass);
                if (mayProcessMembers(psiClass)) {
                    final FilterScopeProcessor<PsiElement> declProcessor = new FilterScopeProcessor<PsiElement>(TrueFilter.INSTANCE);
                    psiClass.processDeclarations(declProcessor, ResolveState.initial(), null, myPlace);
                    doProcessMembers(acceptMethods, results, psiType == baseType, declProcessor.getResults());

                    String name = psiClass.getName();
                    if (name != null && searchFactoryMethods) {
                        Collection<PsiMember> factoryMethods = JavaStaticMemberTypeIndex.getInstance().getStaticMembers(name, project, scope);
                        doProcessMembers(acceptMethods, results, false, factoryMethods);
                    }
                }
            }
        };
        consumer.consume(baseType);
        if (searchInheritors && !CommonClassNames.JAVA_LANG_OBJECT.equals(where.getQualifiedName())) {
            CodeInsightUtil.processSubTypes(baseType, myPlace, true, PrefixMatcher.ALWAYS_TRUE, consumer);
        }
    }

    private static boolean isPrimitiveClass(PsiClass where) {
        String qname = where.getQualifiedName();
        if (qname == null || !qname.startsWith("java.lang.")) return false;
        return CommonClassNames.JAVA_LANG_STRING.equals(qname) || InheritanceUtil.isInheritor(where, CommonClassNames.JAVA_LANG_NUMBER);
    }

    private void doProcessMembers(boolean acceptMethods,
                                  Consumer<LookupElement> results,
                                  boolean isExpectedTypeMember, Collection<? extends PsiElement> declarations) {
        for (final PsiElement result : declarations) {
            if (result instanceof PsiMember && !(result instanceof PsiClass)) {
                final PsiMember member = (PsiMember)result;
                if (!member.hasModifierProperty(PsiModifier.STATIC)) continue;
                if (result instanceof PsiField && !member.hasModifierProperty(PsiModifier.FINAL)) continue;
                if (result instanceof PsiMethod && (!acceptMethods || myPlaceMethods.contains(result))) continue;
                if (JavaCompletionUtil.isInExcludedPackage(member, false) || myImportedStatically.contains(member)) continue;

                if (!JavaPsiFacade.getInstance(myPlace.getProject()).getResolveHelper().isAccessible(member, myPlace, null)) {
                    continue;
                }

                final LookupElement item = result instanceof PsiMethod ? createMethodElement((PsiMethod)result) : createFieldElement((PsiField)result);
                if (item != null) {
                    item.putUserData(EXPECTED_TYPE_MEMBER, isExpectedTypeMember);
                    results.consume(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(item));
                }
            }
        }
    }

    
    protected abstract LookupElement createFieldElement(PsiField field);

    
    protected abstract LookupElement createMethodElement(PsiMethod method);
}
