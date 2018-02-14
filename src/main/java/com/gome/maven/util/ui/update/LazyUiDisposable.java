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
package com.gome.maven.util.ui.update;

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.AsyncResult;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.Consumer;

import javax.swing.*;

public abstract class LazyUiDisposable<T extends Disposable> implements Activatable {

    private Throwable myAllocation;

    private boolean myWasEverShown;

    private final Disposable myParent;
    private final T myChild;

    public LazyUiDisposable( Disposable parent,  JComponent ui,  T child) {
        if (Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("idea.is.internal"))) {
            myAllocation = new Exception();
        }

        myParent = parent;
        myChild = child;

        new UiNotifyConnector.Once(ui, this);
    }

    public final void showNotify() {
        if (myWasEverShown) return;

        try {
            findParentDisposable().doWhenDone(new Consumer<Disposable>() {
                public void consume(Disposable parent) {
                    Project project = null;
                    if (ApplicationManager.getApplication() != null) {
                        project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext());
                    }
                    initialize(parent, myChild, project);
                    Disposer.register(parent, myChild);
                }
            });
        }
        finally {
            myWasEverShown = true;
        }
    }

    public final void hideNotify() {
    }

    protected abstract void initialize( Disposable parent,  T child,  Project project);

    
    private AsyncResult<Disposable> findParentDisposable() {
        return findDisposable(myParent, PlatformDataKeys.UI_DISPOSABLE);
    }


    private static AsyncResult<Disposable> findDisposable(Disposable defaultValue, final DataKey<? extends Disposable> key) {
        if (defaultValue == null) {
            if (ApplicationManager.getApplication() != null) {
                final AsyncResult<Disposable> result = new AsyncResult<Disposable>();
                DataManager.getInstance().getDataContextFromFocus().doWhenDone(new Consumer<DataContext>() {
                    public void consume(DataContext context) {
                        Disposable disposable = key.getData(context);
                        if (disposable == null) {
                            disposable = Disposer.get("ui");
                        }
                        result.setDone(disposable);
                    }
                });
                return result;
            }
            else {
                return null;
            }
        }
        else {
            return new AsyncResult.Done<Disposable>(defaultValue);
        }
    }

}
