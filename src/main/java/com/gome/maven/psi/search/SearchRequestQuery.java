/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.psi.search;

import com.gome.maven.concurrency.AsyncFuture;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.util.AbstractQuery;
import com.gome.maven.util.Processor;

/**
 * @author peter
 */
public class SearchRequestQuery extends AbstractQuery<PsiReference> {
    private final Project myProject;
    private final SearchRequestCollector myRequests;

    public SearchRequestQuery( Project project,  SearchRequestCollector requests) {
        myProject = project;
        myRequests = requests;
    }

    
    @Override
    protected AsyncFuture<Boolean> processResultsAsync( Processor<PsiReference> consumer) {
        return PsiSearchHelper.SERVICE.getInstance(myProject).processRequestsAsync(myRequests, consumer);
    }

    @Override
    protected boolean processResults( Processor<PsiReference> consumer) {
        return PsiSearchHelper.SERVICE.getInstance(myProject).processRequests(myRequests, consumer);
    }

    @Override
    public String toString() {
        return myRequests.toString();
    }
}
