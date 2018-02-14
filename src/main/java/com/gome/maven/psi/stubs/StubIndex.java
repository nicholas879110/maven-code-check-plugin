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
package com.gome.maven.psi.stubs;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.Processor;
import com.gome.maven.util.indexing.FileBasedIndex;
import com.gome.maven.util.indexing.IdFilter;

import java.util.Collection;
import java.util.Iterator;

public abstract class StubIndex {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.stubs.StubIndex");

    private static class StubIndexHolder {
        private static final StubIndex ourInstance = ApplicationManager.getApplication().getComponent(StubIndex.class);
    }
    public static StubIndex getInstance() {
        return StubIndexHolder.ourInstance;
    }

    /**
     * @deprecated use {@link #getElements(StubIndexKey, Object, com.gome.maven.openapi.project.Project, com.gome.maven.psi.search.GlobalSearchScope, Class)}
     */
    public abstract <Key, Psi extends PsiElement> Collection<Psi> get( StubIndexKey<Key, Psi> indexKey,
                                                                       Key key,
                                                                       Project project,
                                                                       final GlobalSearchScope scope);

    /**
     * @deprecated use {@link #getElements(StubIndexKey, Object, com.gome.maven.openapi.project.Project, com.gome.maven.psi.search.GlobalSearchScope, Class)}
     */
    public <Key, Psi extends PsiElement> Collection<Psi> get( StubIndexKey<Key, Psi> indexKey,
                                                              Key key,
                                                              Project project,
                                                              final GlobalSearchScope scope,
                                                             IdFilter filter) {
        return get(indexKey, key, project, scope);
    }

    /**
     * @deprecated use processElements
     */
    public <Key, Psi extends PsiElement> boolean process( StubIndexKey<Key, Psi> indexKey,
                                                          Key key,
                                                          Project project,
                                                          GlobalSearchScope scope,
                                                          Processor<? super Psi> processor) {
        return processElements(indexKey, key, project, scope, (Class<Psi>)PsiElement.class, processor);
    }

    public abstract <Key, Psi extends PsiElement> boolean processElements( StubIndexKey<Key, Psi> indexKey,
                                                                           Key key,
                                                                           Project project,
                                                                           GlobalSearchScope scope,
                                                                          Class<Psi> requiredClass,
                                                                           Processor<? super Psi> processor);

    /**
     * @deprecated use processElements
     */
    public <Key, Psi extends PsiElement> boolean process( StubIndexKey<Key, Psi> indexKey,
                                                          Key key,
                                                          Project project,
                                                          GlobalSearchScope scope,
                                                         @SuppressWarnings("UnusedParameters") IdFilter idFilter,
                                                          Processor<? super Psi> processor) {
        return process(indexKey, key, project, scope, processor);
    }

    public <Key, Psi extends PsiElement> boolean processElements( StubIndexKey<Key, Psi> indexKey,
                                                                  Key key,
                                                                  Project project,
                                                                  GlobalSearchScope scope,
                                                                 IdFilter idFilter,
                                                                  Class<Psi> requiredClass,
                                                                  Processor<? super Psi> processor) {
        return process(indexKey, key, project, scope, processor);
    }

    
    public abstract <Key> Collection<Key> getAllKeys( StubIndexKey<Key, ?> indexKey,  Project project);

    public abstract <K> boolean processAllKeys( StubIndexKey<K, ?> indexKey,  Project project, Processor<K> processor);

    public <K> boolean processAllKeys( StubIndexKey<K, ?> indexKey,  Processor<K> processor,
                                       GlobalSearchScope scope,  IdFilter idFilter) {
        return processAllKeys(indexKey, ObjectUtils.assertNotNull(scope.getProject()), processor);
    }

    /**
     * @deprecated use {@link #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, com.intellij.psi.search.GlobalSearchScope, Class)}
     */
    public <Key, Psi extends PsiElement> Collection<Psi> safeGet( StubIndexKey<Key, Psi> indexKey,
                                                                  Key key,
                                                                  final Project project,
                                                                 final GlobalSearchScope scope,
                                                                  Class<Psi> requiredClass) {
        return getElements(indexKey, key, project, scope, requiredClass);
    }

    public static <Key, Psi extends PsiElement> Collection<Psi> getElements( StubIndexKey<Key, Psi> indexKey,
                                                                             Key key,
                                                                             final Project project,
                                                                             final GlobalSearchScope scope,
                                                                             Class<Psi> requiredClass) {
        return getElements(indexKey, key, project, scope, null, requiredClass);
    }

    public static <Key, Psi extends PsiElement> Collection<Psi> getElements( StubIndexKey<Key, Psi> indexKey,
                                                                             Key key,
                                                                             final Project project,
                                                                             final GlobalSearchScope scope,
                                                                             IdFilter idFilter,
                                                                             Class<Psi> requiredClass) {
        //noinspection deprecation
        Collection<Psi> collection = getInstance().get(indexKey, key, project, scope, idFilter);
        for (Iterator<Psi> iterator = collection.iterator(); iterator.hasNext(); ) {
            Psi psi = iterator.next();
            if (!requiredClass.isInstance(psi)) {
                iterator.remove();

                VirtualFile faultyContainer = PsiUtilCore.getVirtualFile(psi);
                if (faultyContainer != null && faultyContainer.isValid()) {
                    FileBasedIndex.getInstance().requestReindex(faultyContainer);
                }

                getInstance().reportStubPsiMismatch(psi, faultyContainer, requiredClass);
            }
        }

        return collection;
    }

    protected <Psi extends PsiElement> void reportStubPsiMismatch(Psi psi, VirtualFile file, Class<Psi> requiredClass) {
        LOG.error("Invalid stub element type in index: " + file + ". found: " + psi + ". expected: " + requiredClass);
    }
    public abstract void forceRebuild( Throwable e);
}
