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
package com.gome.maven.ui.content;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.ui.ComponentContainer;
import com.gome.maven.openapi.util.BusyObject;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.UserDataHolder;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * Represents a tab or pane displayed in a toolwindow or in another content manager.
 *
 * @see ContentFactory#createContent(javax.swing.JComponent, String, boolean)
 */
public interface Content extends UserDataHolder, ComponentContainer {
    
    String PROP_DISPLAY_NAME = "displayName";
    
    String PROP_ICON = "icon";
    String PROP_ACTIONS = "actions";
     String PROP_DESCRIPTION = "description";
    
    String PROP_COMPONENT = "component";

    String PROP_ALERT = "alerting";

    void setComponent(JComponent component);

    void setPreferredFocusableComponent(JComponent component);

    void setPreferredFocusedComponent(Computable<JComponent> computable);

    void setIcon(Icon icon);

    Icon getIcon();

    void setDisplayName(String displayName);

    String getDisplayName();

    void setTabName(String tabName);

    String getTabName();

    void setToolwindowTitle(String toolwindowTitle);

    String getToolwindowTitle();

    Disposable getDisposer();

    /**
     * @param disposer a Disposable object which dispose() method will be invoked upon this content release.
     */
    void setDisposer(Disposable disposer);

    void setShouldDisposeContent(boolean value);
    boolean shouldDisposeContent();

    String getDescription();

    void setDescription(String description);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    ContentManager getManager();

    boolean isSelected();

    void release();

    boolean isValid();
    boolean isPinned();

    void setPinned(boolean locked);
    boolean isPinnable();
    void setPinnable(boolean pinnable);

    boolean isCloseable();
    void setCloseable(boolean closeable);

    void setActions(ActionGroup actions, String place,  JComponent contextComponent);
    void setSearchComponent( JComponent comp);

    ActionGroup getActions();
     JComponent getSearchComponent();
    String getPlace();
    JComponent getActionsContextComponent();

    void setAlertIcon( AlertIcon icon);
     AlertIcon getAlertIcon();

    void fireAlert();

    
    BusyObject getBusyObject();
    void setBusyObject(BusyObject object);

    String getSeparator();
    void setSeparator(String separator);

    void setPopupIcon(Icon icon);
    Icon getPopupIcon();

    /**
     * @param executionId supposed to identify group of contents (for example "Before Launch" tasks and the main Run Configuration)
     */
    void setExecutionId(long executionId);
    long getExecutionId();
}
