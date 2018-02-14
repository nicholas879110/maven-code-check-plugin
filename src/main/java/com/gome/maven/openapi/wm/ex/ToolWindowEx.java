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
package com.gome.maven.openapi.wm.ex;

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowType;
import com.gome.maven.openapi.wm.impl.InternalDecorator;

import java.beans.PropertyChangeListener;

public interface ToolWindowEx extends ToolWindow {
     String PROP_AVAILABLE = "available";
     String PROP_ICON = "icon";
     String PROP_TITLE = "title";
     String PROP_STRIPE_TITLE = "stripe-title";

    /**
     * Removes specified property change listener.
     *
     * @param l listener to be removed.
     */
    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * @return type of internal decoration of tool window.
     * @throws IllegalStateException
     *          if tool window isn't installed.
     */
    ToolWindowType getInternalType();

    void stretchWidth(int value);

    void stretchHeight(int value);

    InternalDecorator getDecorator();

    void setAdditionalGearActions( ActionGroup additionalGearActions);

    void setTitleActions(AnAction... actions);

    void setUseLastFocusedOnActivation(boolean focus);

    boolean isUseLastFocusedOnActivation();

}
