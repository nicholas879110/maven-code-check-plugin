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

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.PsiSearchScopeUtil;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.Processor;
import com.gome.maven.util.Query;
import com.gome.maven.util.QueryExecutor;
import com.gome.maven.util.containers.Stack;
import gnu.trove.THashSet;

import java.lang.ref.Reference;
import java.util.Set;

/**
 * @author max
 */
public class ClassInheritorsSearch extends ExtensibleQueryFactory<PsiClass, ClassInheritorsSearch.SearchParameters> {
    public static final ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.gome.maven.classInheritorsSearch");
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.search.searches.ClassInheritorsSearch");

    public static final ClassInheritorsSearch INSTANCE = new ClassInheritorsSearch();

    static {
        INSTANCE.registerExecutor(new QueryExecutor<PsiClass, SearchParameters>() {
            @Override
            public boolean execute( final SearchParameters parameters,  final Processor<PsiClass> consumer) {
                final PsiClass baseClass = parameters.getClassToProcess();
                final SearchScope searchScope = parameters.getScope();

                LOG.assertTrue(searchScope != null);

                ProgressIndicator progress = ProgressIndicatorProvider.getGlobalProgressIndicator();
                if (progress != null) {
                    progress.pushState();
                    String className = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                        @Override
                        public String compute() {
                            return baseClass.getName();
                        }
                    });
                    progress.setText(className != null ?
                            PsiBundle.message("psi.search.inheritors.of.class.progress", className) :
                            PsiBundle.message("psi.search.inheritors.progress"));
                }

                boolean result = processInheritors(consumer, baseClass, searchScope, parameters);

                if (progress != null) {
                    progress.popState();
                }

                return result;
            }
        });
    }

    public interface InheritanceChecker {
        boolean checkInheritance( PsiClass subClass,  PsiClass parentClass);

        InheritanceChecker DEFAULT = new InheritanceChecker() {
            @Override
            public boolean checkInheritance( PsiClass subClass,  PsiClass parentClass) {
                return subClass.isInheritor(parentClass, false);
            }
        };
    }

    public static class SearchParameters {
        private final PsiClass myClass;
        private final SearchScope myScope;
        private final boolean myCheckDeep;
        private final boolean myCheckInheritance;
        private final boolean myIncludeAnonymous;
        private final Condition<String> myNameCondition;
        private final InheritanceChecker myInheritanceChecker;

        public SearchParameters( final PsiClass aClass,  SearchScope scope, final boolean checkDeep, final boolean checkInheritance, boolean includeAnonymous) {
            this(aClass, scope, checkDeep, checkInheritance, includeAnonymous, Conditions.<String>alwaysTrue());
        }

        public SearchParameters( final PsiClass aClass,  SearchScope scope, final boolean checkDeep, final boolean checkInheritance,
                                boolean includeAnonymous,  final Condition<String> nameCondition) {
            this(aClass, scope, checkDeep, checkInheritance, includeAnonymous, nameCondition, InheritanceChecker.DEFAULT);
        }

        public SearchParameters( final PsiClass aClass,  SearchScope scope, final boolean checkDeep, final boolean checkInheritance,
                                boolean includeAnonymous,  final Condition<String> nameCondition,  InheritanceChecker inheritanceChecker) {
            myClass = aClass;
            myScope = scope;
            myCheckDeep = checkDeep;
            myCheckInheritance = checkInheritance;
            myIncludeAnonymous = includeAnonymous;
            myNameCondition = nameCondition;
            myInheritanceChecker = inheritanceChecker;
        }

        
        public PsiClass getClassToProcess() {
            return myClass;
        }

         public Condition<String> getNameCondition() {
            return myNameCondition;
        }

        public boolean isCheckDeep() {
            return myCheckDeep;
        }

        public SearchScope getScope() {
            return myScope;
        }

        public boolean isCheckInheritance() {
            return myCheckInheritance;
        }

        public boolean isIncludeAnonymous() {
            return myIncludeAnonymous;
        }
    }

    private ClassInheritorsSearch() {}

    public static Query<PsiClass> search( final PsiClass aClass,  SearchScope scope, final boolean checkDeep, final boolean checkInheritance, boolean includeAnonymous) {
        return search(new SearchParameters(aClass, scope, checkDeep, checkInheritance, includeAnonymous));
    }

    public static Query<PsiClass> search( SearchParameters parameters) {
        return INSTANCE.createUniqueResultsQuery(parameters);
    }

    public static Query<PsiClass> search( final PsiClass aClass,  SearchScope scope, final boolean checkDeep, final boolean checkInheritance) {
        return search(aClass, scope, checkDeep, checkInheritance, true);
    }

    public static Query<PsiClass> search( final PsiClass aClass,  SearchScope scope, final boolean checkDeep) {
        return search(aClass, scope, checkDeep, true);
    }

    public static Query<PsiClass> search( final PsiClass aClass, final boolean checkDeep) {
        return search(aClass, ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
            @Override
            public SearchScope compute() {
                if (!aClass.isValid()) {
                    throw new ProcessCanceledException();
                }
                return aClass.getUseScope();
            }
        }), checkDeep);
    }

    public static Query<PsiClass> search( PsiClass aClass) {
        return search(aClass, true);
    }

    private static boolean processInheritors( final Processor<PsiClass> consumer,
                                              final PsiClass baseClass,
                                              final SearchScope searchScope,
                                              final SearchParameters parameters) {
        if (baseClass instanceof PsiAnonymousClass || isFinal(baseClass)) return true;

        final String qname = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
                return baseClass.getQualifiedName();
            }
        });
        if (CommonClassNames.JAVA_LANG_OBJECT.equals(qname)) {
            Project project = PsiUtilCore.getProjectInReadAction(baseClass);
            return AllClassesSearch.search(searchScope, project, parameters.getNameCondition()).forEach(new Processor<PsiClass>() {
                @Override
                public boolean process(final PsiClass aClass) {
                    ProgressIndicatorProvider.checkCanceled();
                    final String qname1 = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                        @Override
                        
                        public String compute() {
                            return aClass.getQualifiedName();
                        }
                    });
                    return CommonClassNames.JAVA_LANG_OBJECT.equals(qname1) || consumer.process(aClass);
                }
            });
        }

        final Ref<PsiClass> currentBase = Ref.create(null);
        final Stack<Pair<Reference<PsiClass>, String>> stack = new Stack<Pair<Reference<PsiClass>, String>>();
        // there are two sets for memory optimization: it's cheaper to hold FQN than PsiClass
        final Set<String> processedFqns = new THashSet<String>(); // FQN of processed classes if the class has one
        final Set<PsiClass> processed = new THashSet<PsiClass>();   // processed classes without FQN (e.g. anonymous)

        final Processor<PsiClass> processor = new Processor<PsiClass>() {
            @Override
            public boolean process(final PsiClass candidate) {
                ProgressIndicatorProvider.checkCanceled();

                final Ref<Boolean> result = new Ref<Boolean>();
                final String[] fqn = new String[1];
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        fqn[0] = candidate.getQualifiedName();
                        if (parameters.isCheckInheritance() || parameters.isCheckDeep() && !(candidate instanceof PsiAnonymousClass)) {
                            if (!parameters.myInheritanceChecker.checkInheritance(candidate, currentBase.get())) {
                                result.set(true);
                                return;
                            }
                        }

                        if (PsiSearchScopeUtil.isInScope(searchScope, candidate)) {
                            if (candidate instanceof PsiAnonymousClass) {
                                result.set(consumer.process(candidate));
                            }
                            else {
                                final String name = candidate.getName();
                                if (name != null && parameters.getNameCondition().value(name) && !consumer.process(candidate)) result.set(false);
                            }
                        }
                    }
                });
                if (!result.isNull()) return result.get().booleanValue();

                if (parameters.isCheckDeep() && !(candidate instanceof PsiAnonymousClass) && !isFinal(candidate)) {
                    Reference<PsiClass> ref = fqn[0] == null ? createHardReference(candidate) : new SoftReference<PsiClass>(candidate);
                    stack.push(Pair.create(ref, fqn[0]));
                }

                return true;
            }
        };
        stack.push(Pair.create(createHardReference(baseClass), qname));
        final GlobalSearchScope projectScope = GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(baseClass));
        final JavaPsiFacade facade = JavaPsiFacade.getInstance(projectScope.getProject());
        while (!stack.isEmpty()) {
            ProgressIndicatorProvider.checkCanceled();

            Pair<Reference<PsiClass>, String> pair = stack.pop();
            PsiClass psiClass = pair.getFirst().get();
            final String fqn = pair.getSecond();
            if (psiClass == null) {
                psiClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
                    @Override
                    public PsiClass compute() {
                        return facade.findClass(fqn, projectScope);
                    }
                });
                if (psiClass == null) continue;
            }
            if (fqn == null) {
                if (!processed.add(psiClass)) continue;
            }
            else {
                if (!processedFqns.add(fqn)) continue;
            }

            currentBase.set(psiClass);
            if (!DirectClassInheritorsSearch.search(psiClass, projectScope, parameters.isIncludeAnonymous(), false).forEach(processor)) return false;
        }
        return true;
    }

    private static Reference<PsiClass> createHardReference(final PsiClass candidate) {
        return new SoftReference<PsiClass>(candidate){
            @Override
            public PsiClass get() {
                return candidate;
            }
        };
    }

    private static boolean isFinal( final PsiClass baseClass) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return Boolean.valueOf(baseClass.hasModifierProperty(PsiModifier.FINAL));
            }
        }).booleanValue();
    }
}
