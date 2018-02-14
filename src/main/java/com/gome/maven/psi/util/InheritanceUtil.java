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
package com.gome.maven.psi.util;

import com.gome.maven.psi.*;
import com.gome.maven.util.Processor;
import gnu.trove.THashSet;

import java.util.LinkedHashSet;
import java.util.Set;

public class InheritanceUtil {
    private InheritanceUtil() { }

    /**
     * @param aClass     a class to check.
     * @param baseClass  supposed base class.
     * @param checkDeep  true to check deeper than aClass.super (see {@linkplain PsiClass#isInheritor(com.gome.maven.psi.PsiClass, boolean)}).
     * @return true if aClass is the baseClass or baseClass inheritor
     */
    public static boolean isInheritorOrSelf( PsiClass aClass,  PsiClass baseClass, boolean checkDeep) {
        if (aClass == null || baseClass == null) return false;
        PsiManager manager = aClass.getManager();
        return manager.areElementsEquivalent(baseClass, aClass) || aClass.isInheritor(baseClass, checkDeep);
    }

    public static boolean processSupers( PsiClass aClass, boolean includeSelf,  Processor<PsiClass> superProcessor) {
        if (aClass == null) return true;

        if (includeSelf && !superProcessor.process(aClass)) return false;

        return processSupers(aClass, superProcessor, new THashSet<PsiClass>());
    }

    private static boolean processSupers( PsiClass aClass,  Processor<PsiClass> superProcessor,  Set<PsiClass> visited) {
        if (!visited.add(aClass)) return true;

        for (final PsiClass intf : aClass.getInterfaces()) {
            if (!superProcessor.process(intf) || !processSupers(intf, superProcessor, visited)) return false;
        }
        final PsiClass superClass = aClass.getSuperClass();
        if (superClass != null) {
            if (!superProcessor.process(superClass) || !processSupers(superClass, superProcessor, visited)) return false;
        }
        return true;
    }

    public static boolean isInheritor( PsiType type,   final String baseClassName) {
        if (type instanceof PsiClassType) {
            return isInheritor(((PsiClassType)type).resolve(), baseClassName);
        }

        return false;
    }

    
    public static boolean isInheritor( PsiClass psiClass,  final String baseClassName) {
        return isInheritor(psiClass, false, baseClassName);
    }


    public static boolean isInheritor( PsiClass psiClass, final boolean strict,  final String baseClassName) {
        if (psiClass == null) {
            return false;
        }

        final PsiClass base = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(baseClassName, psiClass.getResolveScope());
        if (base == null) {
            return false;
        }

        return strict ? psiClass.isInheritor(base, true) : isInheritorOrSelf(psiClass, base, true);
    }

    /**
     * Gets all superclasses. Classes are added to result in DFS order
     * @param aClass
     * @param results
     * @param includeNonProject
     */
    public static void getSuperClasses( PsiClass aClass,  Set<PsiClass> results, boolean includeNonProject) {
        getSuperClassesOfList(aClass.getSuperTypes(), results, includeNonProject, new THashSet<PsiClass>(), aClass.getManager());
    }

    public static LinkedHashSet<PsiClass> getSuperClasses( PsiClass aClass) {
        LinkedHashSet<PsiClass> result = new LinkedHashSet<PsiClass>();
        getSuperClasses(aClass, result, true);
        return result;
    }


    private static void getSuperClassesOfList( PsiClassType[] types,
                                               Set<PsiClass> results,
                                              boolean includeNonProject,
                                               Set<PsiClass> visited,
                                               PsiManager manager) {
        for (PsiClassType type : types) {
            PsiClass resolved = type.resolve();
            if (resolved != null && visited.add(resolved)) {
                if (includeNonProject || manager.isInProject(resolved)) {
                    results.add(resolved);
                }
                getSuperClassesOfList(resolved.getSuperTypes(), results, includeNonProject, visited, manager);
            }
        }
    }

    public static boolean hasEnclosingInstanceInScope(PsiClass aClass,
                                                      PsiElement scope,
                                                      final boolean isSuperClassAccepted,
                                                      boolean isTypeParamsAccepted) {
        PsiManager manager = aClass.getManager();
        PsiElement place = scope;
        while (place != null && place != aClass && !(place instanceof PsiFile)) {
            if (place instanceof PsiClass) {
                if (isSuperClassAccepted) {
                    if (isInheritorOrSelf((PsiClass)place, aClass, true)) return true;
                }
                else {
                    if (manager.areElementsEquivalent(place, aClass)) return true;
                }
                if (isTypeParamsAccepted && place instanceof PsiTypeParameter) {
                    return true;
                }
            }
            if (place instanceof PsiModifierListOwner) {
                final PsiModifierList modifierList = ((PsiModifierListOwner)place).getModifierList();
                if (modifierList != null && modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                    return false;
                }
            }
            place = place.getParent();
        }
        return place == aClass;
    }
}
