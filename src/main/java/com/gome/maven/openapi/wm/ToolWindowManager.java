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

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.popup.Balloon;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;

/**
 * If you want to register a toolwindow, which will be enabled during the dumb mode, please use {@link ToolWindowManager}'s
 * registration methods which have 'canWorkInDumMode' parameter.
 */
public abstract class ToolWindowManager {

    public abstract boolean canShowNotification( String toolWindowId);

    public static ToolWindowManager getInstance( Project project){
        return project.getComponent(ToolWindowManager.class);
    }

    /**
     * Register specified tool window into IDE window system.
     * @param id <code>id</code> of tool window to be registered.
     * @param component <code>component</code> which represents tool window content.
     * May be null. Content can be further added via content manager for this tool window (See {@link ToolWindow#getContentManager()})
     * @param anchor the default anchor for first registration. It uses only first time the
     * tool window with the specified <code>id</code> is being registered into the window system.
     * After the first registration window's anchor is stored in project file
     * and <code>anchor</code> is ignored.
     * @exception IllegalArgumentException if the same window is already installed or one
     * of the parameters is <code>null</code>.
     * @return tool window
     * @deprecated  {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
     */
    @Deprecated
    public abstract ToolWindow registerToolWindow( String id,  JComponent component,  ToolWindowAnchor anchor);

    /**
     * @deprecated  {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
     */
    @Deprecated
    
    public abstract ToolWindow registerToolWindow( String id,
                                                   JComponent component,
                                                   ToolWindowAnchor anchor,
                                                   Disposable parentDisposable);

    /**
     * @deprecated  {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
     */
    @Deprecated
    
    public abstract ToolWindow registerToolWindow( String id,
                                                   JComponent component,
                                                   ToolWindowAnchor anchor,
                                                  Disposable parentDisposable,
                                                  boolean canWorkInDumbMode);
    /**
     * @deprecated  {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
     */
    @Deprecated
    
    public abstract ToolWindow registerToolWindow( String id,
                                                   JComponent component,
                                                   ToolWindowAnchor anchor,
                                                  Disposable parentDisposable,
                                                  boolean canWorkInDumbMode,
                                                  boolean canCloseContents);

    
    public abstract ToolWindow registerToolWindow( String id, boolean canCloseContent,  ToolWindowAnchor anchor);

    
    public abstract ToolWindow registerToolWindow( String id, boolean canCloseContent,  ToolWindowAnchor anchor, boolean secondary);

    
    public abstract ToolWindow registerToolWindow( String id, boolean canCloseContent,  ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode);

    
    public abstract ToolWindow registerToolWindow( String id, boolean canCloseContent,  ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode, boolean secondary);

    
    public ToolWindow registerToolWindow( final String id,
                                         final boolean canCloseContent,
                                          final ToolWindowAnchor anchor,
                                         final Disposable parentDisposable) {
        return registerToolWindow(id, canCloseContent, anchor, parentDisposable, false);
    }

    /**
     * does nothing if tool window with specified isn't registered.
     */
    public abstract void unregisterToolWindow( String id);

    /**
     */
    public abstract void activateEditorComponent();

    /**
     * @return <code>true</code> if and only if editor component is active.
     */
    public abstract boolean isEditorComponentActive();

    /**
     * @return array of <code>id</code>s of all registered tool windows.
     */
    
    public abstract String[] getToolWindowIds();

    /**
     * @return <code>ID</code> of currently active tool window or <code>null</code> if there is no active
     * tool window.
     */
    
    public abstract String getActiveToolWindowId();

    /**
     * @return registered tool window with specified <code>id</code>. If there is no registered
     * tool window with specified <code>id</code> then the method returns <code>null</code>.
     */
    public abstract ToolWindow getToolWindow(String id);

    /**
     * Puts specified runnable to the tail of current command queue.
     */
    public abstract void invokeLater( Runnable runnable);

    /**
     * Utility method for quick access to the focus manager
     */
    
    public abstract IdeFocusManager getFocusManager();

    public abstract void notifyByBalloon( final String toolWindowId,  final MessageType type,  final String htmlBody);

    public abstract void notifyByBalloon( final String toolWindowId,
                                          final MessageType type,
                                          final String htmlBody,
                                          final Icon icon,
                                          HyperlinkListener listener);

    
    public abstract Balloon getToolWindowBalloon(String id);

    public abstract boolean isMaximized( ToolWindow wnd);

    public abstract void setMaximized( ToolWindow wnd, boolean maximized);
}
