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

import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiMember;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.Query;

/**
 * @author max
 */
public class AnnotatedMembersSearch {

    private AnnotatedMembersSearch() {}

    public static Query<PsiMember> search( PsiClass annotationClass,  SearchScope scope) {
        return AnnotatedElementsSearch.searchPsiMembers(annotationClass, scope);
    }

    public static Query<PsiMember> search( PsiClass annotationClass) {
        return search(annotationClass, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(annotationClass)));
    }
}
