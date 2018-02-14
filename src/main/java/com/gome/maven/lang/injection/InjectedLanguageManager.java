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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jun 8, 2007
 * Time: 8:41:25 PM
 */
package com.gome.maven.lang.injection;

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiLanguageInjectionHost;

import java.util.List;

public abstract class InjectedLanguageManager {

    /** @see com.gome.maven.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME */
    @Deprecated
    public static final ExtensionPointName<MultiHostInjector> MULTIHOST_INJECTOR_EP_NAME = MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME;

    protected static final NotNullLazyKey<InjectedLanguageManager, Project> INSTANCE_CACHE = ServiceManager.createLazyKey(InjectedLanguageManager.class);

    public static InjectedLanguageManager getInstance(Project project) {
        return INSTANCE_CACHE.getValue(project);
    }

    
    public abstract PsiLanguageInjectionHost getInjectionHost( PsiElement element);

    
    public abstract TextRange injectedToHost( PsiElement injectedContext,  TextRange injectedTextRange);
    public abstract int injectedToHost( PsiElement injectedContext, int injectedOffset);

    /**
     * Test-only method.
     * @see com.gome.maven.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME
     */
    @Deprecated
    public abstract void registerMultiHostInjector( MultiHostInjector injector);

    /**
     * Test-only method.
     * @see com.gome.maven.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME
     */
    @Deprecated
    public abstract boolean unregisterMultiHostInjector( MultiHostInjector injector);

    public abstract String getUnescapedText( PsiElement injectedNode);

    
    public abstract List<TextRange> intersectWithAllEditableFragments( PsiFile injectedPsi,  TextRange rangeToEdit);

    public abstract boolean isInjectedFragment( PsiFile file);

    
    public abstract PsiElement findInjectedElementAt( PsiFile hostFile, int hostDocumentOffset);

    
    public abstract List<Pair<PsiElement, TextRange>> getInjectedPsiFiles( PsiElement host);

    public abstract void dropFileCaches( PsiFile file);

    public abstract PsiFile getTopLevelFile( PsiElement element);

    
    public abstract List<DocumentWindow> getCachedInjectedDocuments( PsiFile hostPsiFile);

    public abstract void startRunInjectors( Document hostDocument, boolean synchronously);

    public abstract void enumerate( PsiElement host,  PsiLanguageInjectionHost.InjectedPsiVisitor visitor);
    public abstract void enumerateEx( PsiElement host,  PsiFile containingFile, boolean probeUp,  PsiLanguageInjectionHost.InjectedPsiVisitor visitor);
}
