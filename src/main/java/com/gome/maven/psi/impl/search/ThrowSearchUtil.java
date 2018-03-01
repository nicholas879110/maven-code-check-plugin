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
package com.gome.maven.psi.impl.search;

import com.gome.maven.find.findUsages.FindUsagesOptions;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.searches.MethodReferencesSearch;
import com.gome.maven.psi.util.PsiFormatUtil;
import com.gome.maven.psi.util.PsiFormatUtilBase;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.HashSet;

import java.util.Set;

/**
 * Author: msk
 */
public class ThrowSearchUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.search.ThrowSearchUtil");

    private ThrowSearchUtil() {
    }

    public static class Root {
         private final PsiElement myElement;
         private final PsiType myType;
        private final boolean isExact;

        public Root( PsiElement root,  PsiType type, final boolean exact) {
            myElement = root;
            myType = type;
            isExact = exact;
        }

        @Override
        public String toString() {
            return PsiFormatUtil.formatType(myType, PsiFormatUtilBase.SHOW_FQ_CLASS_NAMES, PsiSubstitutor.EMPTY);
        }
    }

    /**
     * @return true, if we should continue processing
     */
    private static boolean processExn( PsiParameter aCatch,  Processor<UsageInfo> processor,  Root root) {
        final PsiType type = aCatch.getType();
        if (type.isAssignableFrom(root.myType)) {
            processor.process(new UsageInfo(aCatch));
            return false;
        }
        if (!root.isExact && root.myType.isAssignableFrom(type)) {
            processor.process(new UsageInfo(aCatch));
            return true;
        }
        return true;
    }

    private static boolean scanCatches( PsiElement elem,
                                        Processor<UsageInfo> processor,
                                        Root root,
                                        FindUsagesOptions options,
                                        Set<PsiMethod> processed) {
        while (elem != null) {
            final PsiElement parent = elem.getParent();
            if (elem instanceof PsiMethod) {
                final PsiMethod deepestSuperMethod = ((PsiMethod)elem).findDeepestSuperMethod();
                final PsiMethod method = deepestSuperMethod != null ? deepestSuperMethod : (PsiMethod)elem;
                if (!processed.contains(method)) {
                    processed.add(method);
                    final PsiReference[] refs = MethodReferencesSearch.search(method, options.searchScope, true).toArray(PsiReference.EMPTY_ARRAY);
                    for (int i = 0; i != refs.length; ++i) {
                        if (!scanCatches(refs[i].getElement(), processor, root, options, processed)) return false;
                    }
                }
                return true;
            }
            if (elem instanceof PsiTryStatement) {
                final PsiTryStatement aTry = (PsiTryStatement)elem;
                final PsiParameter[] catches = aTry.getCatchBlockParameters();
                for (int i = 0; i != catches.length; ++i) {
                    if (!processExn(catches[i], processor, root)) {
                        return false;
                    }
                }
            }
            else if (parent instanceof PsiTryStatement) {
                final PsiTryStatement tryStmt = (PsiTryStatement)parent;
                if (elem != tryStmt.getTryBlock()) {
                    elem = parent.getParent();
                    continue;
                }
            }
            elem = parent;
        }
        return true;
    }

    public static boolean addThrowUsages( Processor<UsageInfo> processor,  Root root,  FindUsagesOptions options) {
        Set<PsiMethod> processed = new HashSet<PsiMethod>();
        return scanCatches(root.myElement, processor, root, options, processed);
    }

    /**
     * @return is type of exn exactly known
     */

    private static boolean isExactExnType(final PsiExpression exn) {
        return exn instanceof PsiNewExpression;
    }

    
    public static Root[] getSearchRoots(final PsiElement element) {
        if (element instanceof PsiThrowStatement) {
            final PsiThrowStatement aThrow = (PsiThrowStatement)element;
            final PsiExpression exn = aThrow.getException();
            return new Root[]{new Root(aThrow.getParent(), exn.getType(), isExactExnType(exn))};
        }
        if (element instanceof PsiKeyword) {
            final PsiKeyword kwd = (PsiKeyword)element;
            if (PsiKeyword.THROWS.equals(kwd.getText())) {
                final PsiElement parent = kwd.getParent();
                if (parent != null && parent.getParent() instanceof PsiMethod) {
                    final PsiMethod method = (PsiMethod)parent.getParent();
                    final PsiReferenceList throwsList = method.getThrowsList();
                    final PsiClassType[] exns = throwsList.getReferencedTypes();
                    final Root[] roots = new Root[exns.length];
                    for (int i = 0; i != roots.length; ++i) {
                        final PsiClassType exn = exns[i];
                        roots[i] = new Root(method, exn, false); // TODO: test for final
                    }
                    return roots;
                }
            }
        }
        return null;
    }

    public static boolean isSearchable(final PsiElement element) {
        return getSearchRoots(element) != null;
    }

    public static String getSearchableTypeName(final PsiElement e) {
        if (e instanceof PsiThrowStatement) {
            final PsiThrowStatement aThrow = (PsiThrowStatement)e;
            final PsiType type = aThrow.getException().getType();
            return PsiFormatUtil.formatType(type, PsiFormatUtilBase.SHOW_FQ_CLASS_NAMES, PsiSubstitutor.EMPTY);
        }
        if (e instanceof PsiKeyword && PsiKeyword.THROWS.equals(e.getText())) {
            return e.getParent().getText();
        }
        LOG.error("invalid searchable element");
        return e.getText();
    }
}
