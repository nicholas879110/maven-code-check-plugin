/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.psi.impl.source.tree.injected;

import com.gome.maven.lang.injection.MultiHostInjector;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.util.CachedValueProvider;
import com.gome.maven.psi.util.ParameterizedCachedValueProvider;
import com.gome.maven.psi.util.PsiModificationTracker;

import java.util.List;

/**
 * @author cdr
 */
class InjectedPsiCachedValueProvider implements ParameterizedCachedValueProvider<MultiHostRegistrarImpl, PsiElement> {
    @Override
    public CachedValueProvider.Result<MultiHostRegistrarImpl> compute(PsiElement element) {
        PsiFile hostPsiFile = element.getContainingFile();
        if (hostPsiFile == null) return null;
        FileViewProvider viewProvider = hostPsiFile.getViewProvider();
        final DocumentEx hostDocument = (DocumentEx)viewProvider.getDocument();
        if (hostDocument == null) return null;

        PsiManager psiManager = viewProvider.getManager();
        final Project project = psiManager.getProject();
        InjectedLanguageManagerImpl injectedManager = InjectedLanguageManagerImpl.getInstanceImpl(project);

        final MultiHostRegistrarImpl result = doCompute(element, injectedManager, project, hostPsiFile);

        return CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT, hostDocument);
    }

    
    static MultiHostRegistrarImpl doCompute( final PsiElement element,
                                             InjectedLanguageManagerImpl injectedManager,
                                            Project project,
                                            PsiFile hostPsiFile) {
        MyInjProcessor processor = new MyInjProcessor(project, hostPsiFile);
        injectedManager.processInPlaceInjectorsFor(element, processor);
        MultiHostRegistrarImpl registrar = processor.hostRegistrar;
        return registrar == null || registrar.getResult() == null ? null : registrar;
    }

    private static class MyInjProcessor implements InjectedLanguageManagerImpl.InjProcessor {
        private MultiHostRegistrarImpl hostRegistrar;
        private final Project myProject;
        private final PsiFile myHostPsiFile;

        private MyInjProcessor(Project project, PsiFile hostPsiFile) {
            myProject = project;
            myHostPsiFile = hostPsiFile;
        }

        @Override
        public boolean process(PsiElement element, MultiHostInjector injector) {
            if (hostRegistrar == null) {
                hostRegistrar = new MultiHostRegistrarImpl(myProject, myHostPsiFile, element);
            }
            injector.getLanguagesToInject(hostRegistrar, element);
            List<Pair<Place,PsiFile>> result = hostRegistrar.getResult();
            return result == null;
        }
    }
}
