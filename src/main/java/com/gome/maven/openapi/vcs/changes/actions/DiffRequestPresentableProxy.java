/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.vcs.VcsException;

import java.util.Collections;
import java.util.List;

public abstract class DiffRequestPresentableProxy implements DiffRequestPresentable {
    private DiffRequestPresentable myDelegate;
    private List<? extends AnAction> myCachedActions;
    private MyResult myStepResult;

    
    public abstract DiffRequestPresentable init() throws VcsException;

    
    private DiffRequestPresentable initRequest() throws VcsException {
        if (myDelegate == null) {
            myDelegate = init();
        }
        return myDelegate;
    }

    public List<? extends AnAction> createActions(DiffExtendUIFactory uiFactory) {
        if (myCachedActions == null) {
            try {
                myCachedActions = initRequest().createActions(uiFactory);
            }
            catch (VcsException e) {
                //should not occur
                return Collections.emptyList();
            }
        }
        return myCachedActions;
    }

    public MyResult step(DiffChainContext context) {
        final DiffRequestPresentable request;
        try {
            request = initRequest();
            myStepResult = request.step(context);
        }
        catch (VcsException e) {
            myStepResult = new MyResult(null, DiffPresentationReturnValue.quit, e.getMessage());
        }
        return myStepResult;
    }

    public void haveStuff() throws VcsException {
        final DiffRequestPresentable request = initRequest();
        request.haveStuff();
    }
}
