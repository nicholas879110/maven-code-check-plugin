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
package com.gome.maven.openapi.wm;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Expirable;
import com.gome.maven.openapi.util.ExpirableRunnable;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * This class receives focus requests, manages the, and delegates to the awt focus subsystem. All focus requests
 * should be done through this class. For example, to request focus on a component:
 * <pre>
 *   IdeFocusManager.getInstance(project).requestFocus(comp, true);
 * </pre>
 * This is the preferred way to request focus on components to
 * <pre>
 *   comp.requestFocus();
 * </pre>
 *
 * This class is also responsible for delivering key events while focus transferring is in progress.
 * <p>
 * <code>IdeFocusManager</code> instance can be received per project or the global instance. The preferred way is
 * to use instance <code>IdeFocusManager.getInstance(project)</code>. If no project instance is available, then
 * <code>IdeFocusManager.getGlobalInstance()</code> can be used.
 */

public abstract class IdeFocusManager implements FocusRequestor {

    /**
     * Finds most suitable component to request focus to. For instance you may pass a JPanel instance,
     * this method will traverse into it's children to find focusable component
     * @return suitable component to focus
     */
    
    public abstract JComponent getFocusTargetFor( final JComponent comp);


    /**
     * Executes given runnable after all focus activities are finished
     */
    public abstract void doWhenFocusSettlesDown( Runnable runnable);

    /**
     * Executes given runnable after all focus activities are finished
     */
    public abstract void doWhenFocusSettlesDown( ExpirableRunnable runnable);


    /**
     * Finds focused component among descendants of the given component. Descendants may be in child popups and windows
     */
    
    public abstract Component getFocusedDescendantFor(final Component comp);

    /**
     * Dispatches given key event. This methods should not be called by the user code
     * @return true is the event was dispatched, false - otherwise.
     */
    public abstract boolean dispatch( KeyEvent e);

    /**
     * Aggregates all key events until given callback object is processed
     * @param done action callback
     */
    public abstract void typeAheadUntil(ActionCallback done);

    /**
     * Reports if any focus activity is being done
     */
    public abstract boolean isFocusBeingTransferred();

    /**
     * Requests default focus. The method should not be called by the user code.
     */
    
    public abstract ActionCallback requestDefaultFocus(boolean forced);

    /**
     * Reports of focus transfer is enabled right now. It can be disabled if app is inactive. In this case
     * all focus requests will be either postponed or executed only if <code>FocusCommand</code> can be executed on an inaactive app.
     * @see com.gome.maven.openapi.wm.FocusCommand#canExecuteOnInactiveApp()
     */
    public abstract boolean isFocusTransferEnabled();

    /**
     * Returns <code>Expirable</code> instance for the given counter of focus commands. As any new <code>FocusCommand</code>
     * is emitted to execute, the counter increments thus making the returned <code>Expirable</code> objects expired.
     */
    
    public abstract Expirable getTimestamp(boolean trackOnlyForcedCommands);

    /**
     * Returns <code>FocusRequestor</code> object which will emit focus requests unless expired.
     * @see #getTimestamp(boolean)
     */
    
    public abstract FocusRequestor getFurtherRequestor();

    /**
     * Injects some procedure that will maybe do something with focus after all focus requests are fulfilled and
     * before focus transfer is reported ready.
     */
    public abstract void revalidateFocus( ExpirableRunnable runnable);

    /**
     * Enables or disables typeahead
     * @see #typeAheadUntil(com.gome.maven.openapi.util.ActionCallback)
     */
    public abstract void setTypeaheadEnabled(boolean enabled);

    /**
     * Computes effective focus owner
     */
    public abstract Component getFocusOwner();

    /**
     * Runs runnable for whicj <code>DataContext</code> will no be computed from the current focus owner,
     * but used the given one
     */
    public abstract void runOnOwnContext( DataContext context,  Runnable runnable);

    /**
     * Returns last focused component for the given <code>IdeFrame</code>
     */
    
    public abstract Component getLastFocusedFor( IdeFrame frame);

    /**
     * Returns last focused <code>IdeFrame</code>
     */
    
    public abstract IdeFrame getLastFocusedFrame();

    /**
     * Put the container window to front. May not execute of the app is inactive or under some other conditions. This
     * is the preferred way to finding the container window and unconditionally calling <code>window.toFront()</code>
     */
    public abstract void toFront(JComponent c);

    public static IdeFocusManager getInstance( Project project) {
        if (project == null || project.isDisposed() || !project.isInitialized()) return getGlobalInstance();

        return project.getComponent(IdeFocusManager.class);
    }

    
    public static IdeFocusManager findInstanceByContext( DataContext context) {
        IdeFocusManager instance = null;
        if (context != null) {
            instance = getInstanceSafe(CommonDataKeys.PROJECT.getData(context));
        }

        if (instance == null) {
            instance = findByComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow());
        }

        if (instance == null) {
            instance = getGlobalInstance();
        }

        return instance;
    }

    
    public static IdeFocusManager findInstanceByComponent( Component c) {
        final IdeFocusManager instance = findByComponent(c);
        return instance != null ? instance : findInstanceByContext(null);
    }


    
    private static IdeFocusManager findByComponent(Component c) {
        final Component parent = UIUtil.findUltimateParent(c);
        if (parent instanceof IdeFrame) {
            return getInstanceSafe(((IdeFrame)parent).getProject());
        }
        return null;
    }


    
    private static IdeFocusManager getInstanceSafe( Project project) {
        if (project != null && !project.isDisposed() && project.isInitialized()) {
            return getInstance(project);
        }
        return null;
    }

    
    public static IdeFocusManager findInstance() {
        final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner != null ? findInstanceByComponent(owner) : findInstanceByContext(null);
    }

    
    public static IdeFocusManager getGlobalInstance() {
        IdeFocusManager fm = null;

        Application app = ApplicationManager.getApplication();
        if (app != null && app.hasComponent(IdeFocusManager.class)) {
            fm = app.getComponent(IdeFocusManager.class);
        }

        if (fm == null) {
            // happens when app is semi-initialized (e.g. when IDEA server dialog is shown)
            fm = PassThroughIdeFocusManager.getInstance();
        }

        return fm;
    }

}
