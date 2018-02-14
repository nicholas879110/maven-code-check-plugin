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

package com.gome.maven.find.findUsages;

import com.gome.maven.find.FindSettings;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.search.PredefinedSearchScopeProvider;
import com.gome.maven.psi.search.ProjectScope;
import com.gome.maven.psi.search.SearchRequestCollector;
import com.gome.maven.psi.search.SearchScope;

import java.util.List;

public class FindUsagesOptions implements Cloneable {
    
    public SearchScope searchScope;

    public boolean isSearchForTextOccurrences = true;

    public boolean isUsages = false;
    public SearchRequestCollector fastTrack = null;

    public FindUsagesOptions( Project project) {
        this(project, null);
    }

    public FindUsagesOptions( Project project,  final DataContext dataContext) {
        String defaultScopeName = FindSettings.getInstance().getDefaultScopeName();
        List<SearchScope> predefined = PredefinedSearchScopeProvider.getInstance().getPredefinedScopes(project, dataContext, true, false, false,
                false);
        SearchScope resultScope = null;
        for (SearchScope scope : predefined) {
            if (scope.getDisplayName().equals(defaultScopeName)) {
                resultScope = scope;
                break;
            }
        }
        if (resultScope == null) {
            resultScope = ProjectScope.getProjectScope(project);
        }
        searchScope = resultScope;
    }

    public FindUsagesOptions( SearchScope searchScope) {
        this.searchScope = searchScope;
    }

    @Override
    public FindUsagesOptions clone() {
        try {
            return (FindUsagesOptions)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final FindUsagesOptions that = (FindUsagesOptions)o;

        if (isSearchForTextOccurrences != that.isSearchForTextOccurrences) return false;
        if (isUsages != that.isUsages) return false;
        return searchScope.equals(that.searchScope);
    }

    @Override
    public int hashCode() {
        int result = searchScope.hashCode();
        result = 31 * result + (isSearchForTextOccurrences ? 1 : 0);
        result = 31 * result + (isUsages ? 1 : 0);
        return result;
    }

    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "searchScope=" + searchScope +
                ", isSearchForTextOccurrences=" + isSearchForTextOccurrences +
                ", isUsages=" + isUsages +
                '}';
    }

    
    public String generateUsagesString() {
        return "Usages";
    }
}
