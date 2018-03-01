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
package com.gome.maven.psi.search.searches;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.util.Query;
import com.gome.maven.util.QueryExecutor;

/**
 * @author max
 */
public class DeepestSuperMethodsSearch extends ExtensibleQueryFactory<PsiMethod, PsiMethod> {
    public static final ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.gome.maven.deepestSuperMethodsSearch");
    public static final DeepestSuperMethodsSearch DEEPEST_SUPER_METHODS_SEARCH_INSTANCE = new DeepestSuperMethodsSearch();

    private DeepestSuperMethodsSearch() {
    }

    public static Query<PsiMethod> search(PsiMethod method) {
        return DEEPEST_SUPER_METHODS_SEARCH_INSTANCE.createQuery(method);
    }
}
