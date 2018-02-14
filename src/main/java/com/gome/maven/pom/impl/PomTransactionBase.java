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
package com.gome.maven.pom.impl;

import com.gome.maven.pom.PomManager;
import com.gome.maven.pom.PomModelAspect;
import com.gome.maven.pom.PomTransaction;
import com.gome.maven.pom.event.PomModelEvent;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.IncorrectOperationException;

public abstract class PomTransactionBase implements PomTransaction{
    private final PsiElement myScope;
    private final PomModelAspect myAspect;
    private final PomModelEvent myAccumulatedEvent;
    public PomTransactionBase(PsiElement scope, final PomModelAspect aspect){
        myScope = scope;
        myAspect = aspect;
        myAccumulatedEvent = new PomModelEvent(PomManager.getModel(scope.getProject()));
    }

    @Override
    public PomModelEvent getAccumulatedEvent() {
        return myAccumulatedEvent;
    }

    @Override
    public void run() throws IncorrectOperationException {
        // override accumulated event because transaction should construct full model event in its aspect
        final PomModelEvent event = runInner();
        if(event == null){
            // in case of null event aspect change set supposed to be rebuild by low level events
            myAccumulatedEvent.registerChangeSet(myAspect, null);
            return;
        }
        myAccumulatedEvent.merge(event);
    }

    public abstract PomModelEvent runInner() throws IncorrectOperationException;

    @Override
    public PsiElement getChangeScope() {
        return myScope;
    }

    @Override
    public PomModelAspect getTransactionAspect() {
        return myAspect;
    }
}
