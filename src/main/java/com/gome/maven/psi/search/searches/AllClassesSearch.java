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

/*
 * @author max
 */
package com.gome.maven.psi.search.searches;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.util.Query;
import com.gome.maven.util.QueryExecutor;

public class AllClassesSearch extends ExtensibleQueryFactory<PsiClass, AllClassesSearch.SearchParameters> {
    public static ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.gome.maven.allClassesSearch");
    public static final AllClassesSearch INSTANCE = new AllClassesSearch();

    public static class SearchParameters {
        private final SearchScope myScope;
        private final Project myProject;
        private final Condition<String> myShortNameCondition;

        public SearchParameters( SearchScope scope,  Project project) {
            this(scope, project, Conditions.<String>alwaysTrue());
        }

        public SearchParameters( SearchScope scope,  Project project,  Condition<String> shortNameCondition) {
            myScope = scope;
            myProject = project;
            myShortNameCondition = shortNameCondition;
        }

        
        public SearchScope getScope() {
            return myScope;
        }

        
        public Project getProject() {
            return myProject;
        }

        public boolean nameMatches( String name) {
            return myShortNameCondition.value(name);
        }
    }

    
    public static Query<PsiClass> search( SearchScope scope,  Project project) {
        return INSTANCE.createQuery(new SearchParameters(scope, project));
    }

    
    public static Query<PsiClass> search( SearchScope scope,  Project project,  Condition<String> shortNameCondition) {
        return INSTANCE.createQuery(new SearchParameters(scope, project, shortNameCondition));
    }
}