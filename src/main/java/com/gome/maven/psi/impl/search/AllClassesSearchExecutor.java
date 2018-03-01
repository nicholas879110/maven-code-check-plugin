/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * @author max
 */
package com.gome.maven.psi.impl.search;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.PsiShortNamesCache;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.search.searches.AllClassesSearch;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;
import com.gome.maven.util.QueryExecutor;
import com.gome.maven.util.indexing.IdFilter;
import gnu.trove.THashSet;

import java.util.*;

public class AllClassesSearchExecutor implements QueryExecutor<PsiClass, AllClassesSearch.SearchParameters> {
    @Override
    public boolean execute( final AllClassesSearch.SearchParameters queryParameters,  final Processor<PsiClass> consumer) {
        SearchScope scope = queryParameters.getScope();

        if (scope instanceof GlobalSearchScope) {
            return processAllClassesInGlobalScope((GlobalSearchScope)scope, queryParameters, consumer);
        }

        PsiElement[] scopeRoots = ((LocalSearchScope)scope).getScope();
        for (final PsiElement scopeRoot : scopeRoots) {
            if (!processScopeRootForAllClasses(scopeRoot, consumer)) return false;
        }
        return true;
    }

    private static boolean processAllClassesInGlobalScope( final GlobalSearchScope scope,
                                                           final AllClassesSearch.SearchParameters parameters,
                                                           Processor<PsiClass> processor) {
        final Set<String> names = new THashSet<String>(10000);
        processClassNames(parameters.getProject(), scope, new Consumer<String>() {
            @Override
            public void consume(String s) {
                if (parameters.nameMatches(s)) {
                    names.add(s);
                }
            }
        });

        List<String> sorted = new ArrayList<String>(names);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

        return processClassesByNames(parameters.getProject(), scope, sorted, processor);
    }

    public static boolean processClassesByNames(Project project,
                                                final GlobalSearchScope scope,
                                                Collection<String> names,
                                                Processor<PsiClass> processor) {
        final PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        for (final String name : names) {
            ProgressIndicatorProvider.checkCanceled();
            final PsiClass[] classes = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass[]>() {
                @Override
                public PsiClass[] compute() {
                    return cache.getClassesByName(name, scope);
                }
            });
            for (PsiClass psiClass : classes) {
                ProgressIndicatorProvider.checkCanceled();
                if (!processor.process(psiClass)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Project processClassNames(Project project, GlobalSearchScope scope, final Consumer<String> consumer) {
        final ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();

        PsiShortNamesCache.getInstance(project).processAllClassNames(new Processor<String>() {
            int i = 0;

            @Override
            public boolean process(String s) {
                if (indicator != null && i++ % 512 == 0) {
                    indicator.checkCanceled();
                }
                consumer.consume(s);
                return true;
            }
        }, scope, IdFilter.getProjectIdFilter(project, true));

        if (indicator != null) {
            indicator.checkCanceled();
        }
        return project;
    }

    private static boolean processScopeRootForAllClasses( final PsiElement scopeRoot,  final Processor<PsiClass> processor) {
        final boolean[] stopped = {false};

        final JavaElementVisitor visitor = scopeRoot instanceof PsiCompiledElement ? new JavaRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!stopped[0]) {
                    super.visitElement(element);
                }
            }

            @Override
            public void visitClass(PsiClass aClass) {
                stopped[0] = !processor.process(aClass);
                super.visitClass(aClass);
            }
        } : new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!stopped[0]) {
                    super.visitElement(element);
                }
            }

            @Override
            public void visitClass(PsiClass aClass) {
                stopped[0] = !processor.process(aClass);
                super.visitClass(aClass);
            }
        };
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                scopeRoot.accept(visitor);
            }
        });

        return !stopped[0];
    }
}
