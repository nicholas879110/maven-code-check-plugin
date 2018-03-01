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
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.util.CachedValueProvider;
import com.gome.maven.psi.util.CachedValuesManager;
import com.gome.maven.psi.util.InheritanceUtil;
import com.gome.maven.psi.util.PsiModificationTracker;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.util.Map;
import java.util.Set;

public class InheritanceImplUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.InheritanceImplUtil");

    public static boolean isInheritor( final PsiClass candidateClass,  PsiClass baseClass, final boolean checkDeep) {
        if (baseClass instanceof PsiAnonymousClass) return false;
        if (!checkDeep) {
            return isInheritor(candidateClass.getManager(), candidateClass, baseClass, false, null);
        }

        if (hasObjectQualifiedName(candidateClass)) return false;
        if (hasObjectQualifiedName(baseClass)) return true;
        Map<PsiClass, Boolean> map = CachedValuesManager.
                getCachedValue(candidateClass, new CachedValueProvider<Map<PsiClass, Boolean>>() {
                    
                    @Override
                    public Result<Map<PsiClass, Boolean>> compute() {
                        final Map<PsiClass, Boolean> map = ContainerUtil.createConcurrentWeakMap();
                        return Result.create(map, candidateClass, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
                    }
                });

        Boolean computed = map.get(baseClass);
        if (computed == null) {
            computed = isInheritor(candidateClass.getManager(), candidateClass, baseClass, true, null);
            map.put(baseClass, computed);
        }
        return computed;
    }

    public static boolean hasObjectQualifiedName( PsiClass candidateClass) {
        if (!CommonClassNames.JAVA_LANG_OBJECT_SHORT.equals(candidateClass.getName())) {
            return false;
        }
        PsiElement parent = candidateClass.getParent();
        return parent instanceof PsiJavaFile && CommonClassNames.DEFAULT_PACKAGE.equals(((PsiJavaFile)parent).getPackageName());
    }

    private static boolean isInheritor( PsiManager manager,
                                        PsiClass candidateClass,
                                        PsiClass baseClass,
                                       boolean checkDeep,
                                        Set<PsiClass> checkedClasses) {
        if (candidateClass instanceof PsiAnonymousClass) {
            final PsiClass baseCandidateClass = ((PsiAnonymousClass)candidateClass).getBaseClassType().resolve();
            return baseCandidateClass != null && InheritanceUtil.isInheritorOrSelf(baseCandidateClass, baseClass, checkDeep);
        }
    /* //TODO fix classhashprovider so it doesn't use class qnames only
    final ClassHashProvider provider = getHashProvider((PsiManagerImpl) manager);
    if (checkDeep && provider != null) {
      try {
        return provider.isInheritor(baseClass, candidateClass);
      }
      catch (ClassHashProvider.OutOfRangeException e) {
      }
    }
    */
        if(checkDeep && LOG.isDebugEnabled()){
            LOG.debug("Using uncached version for " + candidateClass.getQualifiedName() + " and " + baseClass);
        }

        if (hasObjectQualifiedName(baseClass)) {
            PsiClass objectClass = JavaPsiFacade.getInstance(manager.getProject()).findClass(CommonClassNames.JAVA_LANG_OBJECT, candidateClass.getResolveScope());
            if (manager.areElementsEquivalent(baseClass, objectClass)) {
                if (manager.areElementsEquivalent(candidateClass, objectClass)) return false;
                if (checkDeep || candidateClass.isInterface()) return true;
                return manager.areElementsEquivalent(candidateClass.getSuperClass(), objectClass);
            }
        }

        if (!checkDeep) {
            final boolean cInt = candidateClass.isInterface();
            final boolean bInt = baseClass.isInterface();

            if (candidateClass instanceof PsiCompiledElement) {
                String baseQName = baseClass.getQualifiedName();
                if (baseQName == null) return false;

                GlobalSearchScope scope = candidateClass.getResolveScope();
                if (cInt == bInt && checkReferenceListWithQualifiedNames(baseQName, candidateClass.getExtendsList(), manager, scope)) return true;
                return bInt && !cInt && checkReferenceListWithQualifiedNames(baseQName, candidateClass.getImplementsList(), manager, scope);
            }
            String baseName = baseClass.getName();
            if (cInt == bInt) {
                for (PsiClassType type : candidateClass.getExtendsListTypes()) {
                    if (Comparing.equal(type.getClassName(), baseName)) {
                        if (manager.areElementsEquivalent(baseClass, type.resolve())) {
                            return true;
                        }
                    }
                }
            }
            else if (!cInt) {
                for (PsiClassType type : candidateClass.getImplementsListTypes()) {
                    if (Comparing.equal(type.getClassName(), baseName)) {
                        if (manager.areElementsEquivalent(baseClass, type.resolve())) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        return isInheritorWithoutCaching(manager, candidateClass, baseClass, checkedClasses);
    }

    private static boolean checkReferenceListWithQualifiedNames(final String baseQName, final PsiReferenceList extList, final PsiManager manager,
                                                                final GlobalSearchScope scope) {
        if (extList != null) {
            final PsiJavaCodeReferenceElement[] refs = extList.getReferenceElements();
            for (PsiJavaCodeReferenceElement ref : refs) {
                if (Comparing.equal(PsiNameHelper.getQualifiedClassName(ref.getQualifiedName(), false), baseQName) && JavaPsiFacade
                        .getInstance(manager.getProject()).findClass(baseQName, scope) != null)
                    return true;
            }
        }
        return false;
    }

    private static boolean isInheritorWithoutCaching( PsiManager manager,
                                                      PsiClass aClass,
                                                      PsiClass baseClass,
                                                      Set<PsiClass> checkedClasses) {
        if (manager.areElementsEquivalent(aClass, baseClass)) return false;

        if (aClass.isInterface() && !baseClass.isInterface()) {
            return false;
        }

        if (checkedClasses == null) {
            checkedClasses = new THashSet<PsiClass>();
        }
        checkedClasses.add(aClass);

        return checkInheritor(manager, aClass.getExtendsListTypes(), baseClass, checkedClasses) ||
                checkInheritor(manager, aClass.getImplementsListTypes(), baseClass, checkedClasses);
    }

    private static boolean checkInheritor( PsiManager manager,
                                           PsiClassType[] supers,
                                           PsiClass baseClass,
                                           Set<PsiClass> checkedClasses) {
        for (PsiClassType aSuper : supers) {
            PsiClass aClass = aSuper.resolve();
            if (aClass != null && checkInheritor(manager, aClass, baseClass, checkedClasses)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkInheritor( PsiManager manager,
                                           PsiClass aClass,
                                           PsiClass baseClass,
                                           Set<PsiClass> checkedClasses) {
        ProgressIndicatorProvider.checkCanceled();
        if (manager.areElementsEquivalent(baseClass, aClass)) {
            return true;
        }
        if (checkedClasses.contains(aClass)) { // to prevent infinite recursion
            return false;
        }
        return isInheritor(manager, aClass, baseClass, true, checkedClasses);
    }

    public static boolean isInheritorDeep( PsiClass candidateClass,  PsiClass baseClass,  final PsiClass classToByPass) {
        if (baseClass instanceof PsiAnonymousClass) {
            return false;
        }

        Set<PsiClass> checkedClasses = null;
        if (classToByPass != null) {
            checkedClasses = new THashSet<PsiClass>();
            checkedClasses.add(classToByPass);
        }
        return isInheritor(candidateClass.getManager(), candidateClass, baseClass, true, checkedClasses);
    }
}
