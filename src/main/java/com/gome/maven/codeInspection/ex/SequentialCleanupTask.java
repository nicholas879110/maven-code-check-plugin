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
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.SequentialModalProgressTask;
import com.gome.maven.util.SequentialTask;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

class SequentialCleanupTask implements SequentialTask {

    private final Project myProject;
    private final LinkedHashMap<PsiFile, List<HighlightInfo>> myResults;
    private Iterator<PsiFile> myFileIterator;
    private final SequentialModalProgressTask myProgressTask;
    private int myCount = 0;

    public SequentialCleanupTask(Project project, LinkedHashMap<PsiFile, List<HighlightInfo>> results, SequentialModalProgressTask task) {
        myProject = project;
        myResults = results;
        myProgressTask = task;
        myFileIterator = myResults.keySet().iterator();
    }

    @Override
    public void prepare() {}

    @Override
    public boolean isDone() {
        return myFileIterator == null || !myFileIterator.hasNext();
    }

    @Override
    public boolean iteration() {
        final ProgressIndicator indicator = myProgressTask.getIndicator();
        if (indicator != null) {
            indicator.setFraction((double) myCount++/myResults.size());
        }
        final PsiFile file = myFileIterator.next();
        final List<HighlightInfo> infos = myResults.get(file);
        Collections.reverse(infos); //sort bottom - top
        for (HighlightInfo info : infos) {
            for (final Pair<HighlightInfo.IntentionActionDescriptor, TextRange> actionRange : info.quickFixActionRanges) {
                actionRange.getFirst().getAction().invoke(myProject, null, file);
            }
        }
        return true;
    }

    @Override
    public void stop() {
        myFileIterator = null;
    }
}
