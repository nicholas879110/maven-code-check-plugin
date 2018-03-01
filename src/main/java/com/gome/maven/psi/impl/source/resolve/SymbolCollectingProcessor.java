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
package com.gome.maven.psi.impl.source.resolve;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiAnchor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.ResolveState;
import com.gome.maven.psi.scope.BaseScopeProcessor;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.JavaScopeProcessorEvent;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.util.containers.MostlySingularMultiMap;

/**
 * @author max
 */
public class SymbolCollectingProcessor extends BaseScopeProcessor implements ElementClassHint {
    private final MostlySingularMultiMap<String, ResultWithContext> myResult = new MostlySingularMultiMap<String, ResultWithContext>();
    private PsiElement myCurrentFileContext = null;

    @Override
    public <T> T getHint( Key<T> hintKey) {
        if (hintKey == ElementClassHint.KEY) {
            //noinspection unchecked
            return (T)this;
        }
        return null;
    }

    @Override
    public void handleEvent( PsiScopeProcessor.Event event, Object associated) {
        if (event == JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT) {
            myCurrentFileContext = (PsiElement)associated;
        }
    }

    @Override
    public boolean execute( PsiElement element,  ResolveState state) {
        if (element instanceof PsiNamedElement && element.isValid()) {
            PsiNamedElement named = (PsiNamedElement)element;
            String name = named.getName();
            if (name != null) {
                myResult.add(name, new ResultWithContext(named, myCurrentFileContext));
            }
        }
        return true;
    }

    @Override
    public boolean shouldProcess(DeclarationKind kind) {
        return kind == DeclarationKind.CLASS || kind == DeclarationKind.PACKAGE || kind == DeclarationKind.METHOD || kind == DeclarationKind.FIELD;
    }

    public MostlySingularMultiMap<String, ResultWithContext> getResults() {
        return myResult;
    }

    public static class ResultWithContext {
        private final PsiAnchor myElement;
        private final PsiAnchor myFileContext;

        public ResultWithContext( PsiNamedElement element, PsiElement fileContext) {
            myElement = PsiAnchor.create(element);
            myFileContext = fileContext == null ? null : PsiAnchor.create(fileContext);
        }

        
        public PsiNamedElement getElement() {
            PsiElement element = myElement.retrieve();
            if (element == null) {
                String message = "Anchor hasn't survived: " + myElement;
                if (myElement instanceof PsiAnchor.StubIndexReference) {
                    message += "; diagnostics=" + ((PsiAnchor.StubIndexReference)myElement).diagnoseNull();
                }
                throw new AssertionError(message);
            }

            return (PsiNamedElement)element;
        }

        public PsiElement getFileContext() {
            return myFileContext == null ? null : myFileContext.retrieve();
        }

        @Override
        public String toString() {
            return myElement.toString();
        }
    }
}
