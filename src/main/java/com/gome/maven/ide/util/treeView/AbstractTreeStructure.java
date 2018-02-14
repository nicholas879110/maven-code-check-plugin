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

package com.gome.maven.ide.util.treeView;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.AsyncResult;
import com.gome.maven.psi.PsiDocumentManager;

public abstract class AbstractTreeStructure {
    public abstract Object getRootElement();
    public abstract Object[] getChildElements(Object element);
    
    public abstract Object getParentElement(Object element);

    
    public abstract NodeDescriptor createDescriptor(Object element, NodeDescriptor parentDescriptor);

    public abstract void commit();
    public abstract boolean hasSomethingToCommit();

    
    public static ActionCallback asyncCommitDocuments( Project project) {
        if (project.isDisposed()) return new ActionCallback.Done();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        if (!documentManager.hasUncommitedDocuments()) {
            return new ActionCallback.Done();
        }
        final ActionCallback callback = new ActionCallback();
        documentManager.performWhenAllCommitted(callback.createSetDoneRunnable());
        return callback;
    }

    /**
     * @return callback which is set to {@link ActionCallback#setDone()} when the tree structure is committed.
     * By default it just calls {@link #commit()} synchronously but it is desirable to override it
     * to provide asynchronous commit to the tree structure to make it more responsible.
     * E.g. when you should commit all documents during the {@link #commit()},
     * you can use {@link #asyncCommitDocuments(Project)} to do it asynchronously.
     */
    
    public ActionCallback asyncCommit() {
        if (hasSomethingToCommit()) commit();
        return new ActionCallback.Done();
    }

    public boolean isToBuildChildrenInBackground(Object element){
        return false;
    }

    public boolean isValid(Object element) {
        return true;
    }

    public boolean isAlwaysLeaf(Object element) {
        return false;
    }

    public AsyncResult<Object> revalidateElement(Object element) {
        return new AsyncResult.Done<Object>(element);
    }
}