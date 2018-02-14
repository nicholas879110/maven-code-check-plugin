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

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.util.Disposer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.lang.ref.WeakReference;

public class UiNotifyConnector implements Disposable, HierarchyListener{

    
    private final WeakReference<Component> myComponent;
    private Activatable myTarget;

    public UiNotifyConnector( final Component component,  final Activatable target) {
        myComponent = new WeakReference<Component>(component);
        myTarget = target;
        if (component.isShowing()) {
            showNotify();
        } else {
            hideNotify();
        }
        component.addHierarchyListener(this);
    }

    public void hierarchyChanged( HierarchyEvent e) {
        if (isDisposed()) return;

        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0) {
            final Runnable runnable = new DumbAwareRunnable() {
                public void run() {
                    final Component c = myComponent.get();
                    if (isDisposed() || c == null) return;

                    if (c.isShowing()) {
                        showNotify();
                    }
                    else {
                        hideNotify();
                    }
                }
            };
            final Application app = ApplicationManager.getApplication();
            if (app != null && app.isDispatchThread()) {
                app.invokeLater(runnable, ModalityState.current());
            } else {
                //noinspection SSBasedInspection
                SwingUtilities.invokeLater(runnable);
            }
        }
    }

    protected void hideNotify() {
        myTarget.hideNotify();
    }

    protected void showNotify() {
        myTarget.showNotify();
    }

    public void dispose() {
        if (isDisposed()) return;

        myTarget.hideNotify();
        final Component c = myComponent.get();
        if (c != null) {
            c.removeHierarchyListener(this);
        }

        myTarget = null;
        myComponent.clear();
    }

    private boolean isDisposed() {
        return myTarget == null;
    }

    public static class Once extends UiNotifyConnector {

        private boolean myShown;
        private boolean myHidden;

        public Once(final Component component, final Activatable target) {
            super(component, target);
        }

        protected final void hideNotify() {
            super.hideNotify();
            myHidden = true;
            disposeIfNeeded();
        }

        protected final void showNotify() {
            super.showNotify();
            myShown = true;
            disposeIfNeeded();
        }

        private void disposeIfNeeded() {
            if (myShown && myHidden) {
                Disposer.dispose(this);
            }
        }
    }

    public static void doWhenFirstShown( JComponent c,  final Runnable runnable) {
        new Once(c, new Activatable() {
            public void showNotify() {
                runnable.run();
            }

            public void hideNotify() {
            }
        });
    }

}
