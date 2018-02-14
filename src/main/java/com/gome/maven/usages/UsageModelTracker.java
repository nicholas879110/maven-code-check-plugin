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
package com.gome.maven.usages;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author max
 */
public class UsageModelTracker implements Disposable {
    public interface UsageModelTrackerListener {
        void modelChanged(boolean isPropertyChange);
    }

    private final List<UsageModelTrackerListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    public UsageModelTracker( Project project) {
        final PsiTreeChangeListener myPsiListener = new PsiTreeChangeAdapter() {
            @Override
            public void childAdded( PsiTreeChangeEvent event) {
                doFire(event, false);
            }

            @Override
            public void childRemoved( PsiTreeChangeEvent event) {
                doFire(event, false);
            }

            @Override
            public void childReplaced( PsiTreeChangeEvent event) {
                doFire(event, false);
            }

            @Override
            public void childrenChanged( PsiTreeChangeEvent event) {
                doFire(event, false);
            }

            @Override
            public void childMoved( PsiTreeChangeEvent event) {
                doFire(event, false);
            }

            @Override
            public void propertyChanged( PsiTreeChangeEvent event) {
                doFire(event, true);
            }
        };
        PsiManager.getInstance(project).addPsiTreeChangeListener(myPsiListener, this);
    }

    private void doFire( PsiTreeChangeEvent event, boolean propertyChange) {
        if (!(event.getFile() instanceof PsiCodeFragment)) {
            for (UsageModelTrackerListener listener : myListeners) {
                listener.modelChanged(propertyChange);
            }
        }
    }

    @Override
    public void dispose() {
    }

    public void addListener( UsageModelTrackerListener listener) {
        myListeners.add(listener);
    }

    public void removeListener( UsageModelTrackerListener listener) {
        myListeners.remove(listener);
    }
}
