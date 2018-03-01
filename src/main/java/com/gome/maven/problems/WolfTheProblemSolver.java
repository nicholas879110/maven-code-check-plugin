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

package com.gome.maven.problems;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Collection;
import java.util.List;

/**
 * @author cdr
 */
public abstract class WolfTheProblemSolver {
    public static final ExtensionPointName<Condition<VirtualFile>> FILTER_EP_NAME = ExtensionPointName.create("com.gome.maven.problemFileHighlightFilter");

    public static WolfTheProblemSolver getInstance(Project project) {
        return project.getComponent(WolfTheProblemSolver.class);
    }

    public abstract boolean isProblemFile(VirtualFile virtualFile);

    public abstract void weHaveGotProblems( VirtualFile virtualFile,  List<Problem> problems);
    public abstract void weHaveGotNonIgnorableProblems( VirtualFile virtualFile,  List<Problem> problems);
    public abstract void clearProblems( VirtualFile virtualFile);

    public abstract boolean hasProblemFilesBeneath( Condition<VirtualFile> condition);

    public abstract boolean hasProblemFilesBeneath( Module scope);

    public abstract Problem convertToProblem(VirtualFile virtualFile, int line, int column, String[] message);

    public abstract void reportProblems(final VirtualFile file, Collection<Problem> problems);

    public abstract boolean hasSyntaxErrors(final VirtualFile file);

    public abstract static class ProblemListener {
        public void problemsAppeared( VirtualFile file) {}
        public void problemsChanged( VirtualFile file) {}
        public void problemsDisappeared( VirtualFile file) {}
    }

    public abstract void addProblemListener( ProblemListener listener);
    public abstract void addProblemListener( ProblemListener listener,  Disposable parentDisposable);
    public abstract void removeProblemListener( ProblemListener listener);

    /**
     * @deprecated register extensions to {@link #FILTER_EP_NAME} instead
     */
    public abstract void registerFileHighlightFilter( Condition<VirtualFile> filter,  Disposable parentDisposable);
    public abstract void queue(VirtualFile suspiciousFile);
}
