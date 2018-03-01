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
package com.gome.maven.psi.search.searches;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.search.*;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ContainerUtil;

/**
 * @author max
 */
public class MethodReferencesSearch extends ExtensibleQueryFactory<PsiReference, MethodReferencesSearch.SearchParameters> {
    public static final ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.gome.maven.methodReferencesSearch");
    public static final MethodReferencesSearch INSTANCE = new MethodReferencesSearch();

    public static class SearchParameters {
        private final PsiMethod myMethod;
        private final SearchScope myScope;
        private final boolean myStrictSignatureSearch;
        private final SearchRequestCollector myOptimizer;
        private final boolean isSharedOptimizer;

        public SearchParameters( PsiMethod method,  SearchScope scope, boolean strictSignatureSearch,  SearchRequestCollector optimizer) {
            myMethod = method;
            myScope = scope;
            myStrictSignatureSearch = strictSignatureSearch;
            isSharedOptimizer = optimizer != null;
            myOptimizer = optimizer != null ? optimizer : new SearchRequestCollector(new SearchSession());
        }

        public SearchParameters( PsiMethod method,  SearchScope scope, final boolean strict) {
            this(method, scope, strict, null);
        }

        
        public PsiMethod getMethod() {
            return myMethod;
        }

        public boolean isStrictSignatureSearch() {
            return myStrictSignatureSearch;
        }

        public SearchRequestCollector getOptimizer() {
            return myOptimizer;
        }

        /**
         * @return the user-visible search scope, most often "Project Files" or "Project and Libraries".
         * Searchers most likely need to use {@link #getEffectiveSearchScope()}.
         */
        public SearchScope getScopeDeterminedByUser() {
            return myScope;
        }


        /**
         * @return Same as {@link #getScopeDeterminedByUser()}. Searchers most likely need to use {@link #getEffectiveSearchScope()}.
         */
        @Deprecated
        
        public SearchScope getScope() {
            return getScopeDeterminedByUser();
        }

        
        public SearchScope getEffectiveSearchScope () {
            SearchScope accessScope = PsiSearchHelper.SERVICE.getInstance(myMethod.getProject()).getUseScope(myMethod);
            return myScope.intersectWith(accessScope);
        }
    }

    private MethodReferencesSearch() {}

    public static Query<PsiReference> search( PsiMethod method, SearchScope scope, final boolean strictSignatureSearch) {
        return search(new SearchParameters(method, scope, strictSignatureSearch));
    }

    public static void searchOptimized(final PsiMethod method, SearchScope scope, final boolean strictSignatureSearch,
                                        SearchRequestCollector collector, final Processor<PsiReference> processor) {
        searchOptimized(method, scope, strictSignatureSearch, collector, false, new PairProcessor<PsiReference, SearchRequestCollector>() {
            @Override
            public boolean process(PsiReference psiReference, SearchRequestCollector collector) {
                return processor.process(psiReference);
            }
        });
    }
    public static void searchOptimized(final PsiMethod method, SearchScope scope, final boolean strictSignatureSearch, SearchRequestCollector collector, final boolean inReadAction, PairProcessor<PsiReference, SearchRequestCollector> processor) {
        final SearchRequestCollector nested = new SearchRequestCollector(collector.getSearchSession());
        collector.searchQuery(new QuerySearchRequest(search(new SearchParameters(method, scope, strictSignatureSearch, nested)), nested,
                inReadAction, processor));
    }

    public static Query<PsiReference> search(final SearchParameters parameters) {
        final Query<PsiReference> result = INSTANCE.createQuery(parameters);
        if (parameters.isSharedOptimizer) {
            return uniqueResults(result);
        }

        final SearchRequestCollector requests = parameters.getOptimizer();

        Project project = PsiUtilCore.getProjectInReadAction(parameters.getMethod());
        return uniqueResults(new MergeQuery<PsiReference>(result, new SearchRequestQuery(project, requests)));
    }

    public static Query<PsiReference> search(final PsiMethod method, final boolean strictSignatureSearch) {
        return search(method, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(method)), strictSignatureSearch);
    }

    public static Query<PsiReference> search(final PsiMethod method) {
        return search(method, true);
    }

    private static UniqueResultsQuery<PsiReference, ReferenceDescriptor> uniqueResults( Query<PsiReference> composite) {
        return new UniqueResultsQuery<PsiReference, ReferenceDescriptor>(composite, ContainerUtil.<ReferenceDescriptor>canonicalStrategy(), ReferenceDescriptor.MAPPER);
    }
}