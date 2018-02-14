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
package com.gome.maven.openapi.wm;

import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.BusyObject;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.ui.content.ContentManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;

public interface ToolWindow extends BusyObject {

    Key<Boolean> SHOW_CONTENT_ICON = new Key<Boolean>("ContentIcon");

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    boolean isActive();

    /**
     * @param runnable A command to execute right after the window gets activated.  The call is asynchronous since it may require animation.
     * @exception IllegalStateException if tool window isn't installed.
     */
    void activate( Runnable runnable);

    void activate( Runnable runnable, boolean autoFocusContents);

    void activate( Runnable runnable, boolean autoFocusContents, boolean forced);

    /**
     * @return whether the tool window is visible or not.
     * @exception IllegalStateException if tool window isn't installed.
     */
    boolean isVisible();

    /**
     * @param runnable A command to execute right after the window shows up.  The call is asynchronous since it may require animation.
     * @exception IllegalStateException if tool window isn't installed.
     */
    void show( Runnable runnable);

    /**
     * Hides tool window. If the window is active then the method deactivates it.
     * Does nothing if tool window isn't visible.
     * @param runnable A command to execute right after the window hides.  The call is asynchronous since it may require animation.
     * @exception IllegalStateException if tool window isn't installed.
     */
    void hide( Runnable runnable);

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    ToolWindowAnchor getAnchor();

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    void setAnchor(ToolWindowAnchor anchor,  Runnable runnable);

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    boolean isSplitMode();

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    void setSplitMode(boolean split,  Runnable runnable);

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    boolean isAutoHide();

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    void setAutoHide(boolean state);

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    ToolWindowType getType();

    /**
     * @exception IllegalStateException if tool window isn't installed.
     */
    void setType(ToolWindowType type,  Runnable runnable);

    /**
     * @return window icon. Returns <code>null</code> if window has no icon.
     */
    Icon getIcon();

    /**
     * Sets new window icon.
     */
    void setIcon(Icon icon);

    /**
     * @return window title. Returns <code>null</code> if window has no title.
     */
    String getTitle();

    /**
     * Sets new window title.
     */
    void setTitle(String title);

    /**
     * @return window stripe button text.
     */
    
    String getStripeTitle();

    /**
     * Sets new window stripe button text.
     */
    void setStripeTitle( String title);

    /**
     * @return whether the window is available or not.
     */
    boolean isAvailable();

    /**
     * Sets whether the tool window available or not. Term "available" means that tool window
     * can be shown and it has button on tool window bar.
     * @exception IllegalStateException if tool window isn't installed.
     */
    void setAvailable(boolean available,  Runnable runnable);

    void setContentUiType(ToolWindowContentUiType type,  Runnable runnable);
    void setDefaultContentUiType( ToolWindowContentUiType type);

    ToolWindowContentUiType getContentUiType();

    void installWatcher(ContentManager contentManager);

    /**
     * @return component which represents window content.
     */
    JComponent getComponent();


    ContentManager getContentManager();


    void setDefaultState( ToolWindowAnchor anchor,  ToolWindowType type,  Rectangle floatingBounds);


    void setToHideOnEmptyContent(boolean hideOnEmpty);

    boolean isToHideOnEmptyContent();

    boolean isDisposed();

    void showContentPopup(InputEvent inputEvent);

    ActionCallback getActivation();

    class Border extends EmptyBorder {
        public Border() {
            this(true, true, true, true);
        }

        public Border(boolean top, boolean left, boolean right, boolean bottom) {
            super(top ? 2 : 0, left ? 2 : 0, right ? 2 : 0, bottom ? 2 : 0);
        }
    }

}
