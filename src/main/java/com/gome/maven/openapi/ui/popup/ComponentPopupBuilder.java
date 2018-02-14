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
package com.gome.maven.openapi.ui.popup;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.ui.ActiveComponent;
import com.gome.maven.util.BooleanFunction;
import com.gome.maven.util.Processor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author max
 */
public interface ComponentPopupBuilder {
    
    ComponentPopupBuilder setTitle(String title);

    
    ComponentPopupBuilder setResizable(boolean forceResizable);

    
    ComponentPopupBuilder setMovable(boolean forceMovable);

    
    ComponentPopupBuilder setRequestFocus(boolean requestFocus);

    
    ComponentPopupBuilder setFocusable(boolean focusable);

    
    ComponentPopupBuilder setRequestFocusCondition(Project project, Condition<Project> condition);

    /**
     * @see com.gome.maven.openapi.util.DimensionService
     */
    
    ComponentPopupBuilder setDimensionServiceKey( Project project,  String key, boolean useForXYLocation);

    
    ComponentPopupBuilder setCancelCallback(Computable<Boolean> shouldProceed);

    
    ComponentPopupBuilder setCancelOnClickOutside(boolean cancel);

    
    ComponentPopupBuilder addListener(JBPopupListener listener);

    
    ComponentPopupBuilder setCancelOnMouseOutCallback(MouseChecker shouldCancel);

    
    JBPopup createPopup();

    
    ComponentPopupBuilder setCancelButton( IconButton cancelButton);

    
    ComponentPopupBuilder setCancelOnOtherWindowOpen(boolean cancelOnWindow);

    
    ComponentPopupBuilder setTitleIcon( ActiveIcon icon);

    
    ComponentPopupBuilder setCancelKeyEnabled(boolean enabled);

    
    ComponentPopupBuilder setLocateByContent(boolean byContent);

    
    ComponentPopupBuilder setLocateWithinScreenBounds(boolean within);

    
    ComponentPopupBuilder setMinSize(Dimension minSize);

    /**
     * Use this method to customize shape of popup window (e.g. to use bounded corners).
     */
    @SuppressWarnings("UnusedDeclaration")//used in 'Presentation Assistant' plugin
    
    ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider);

    
    ComponentPopupBuilder setAlpha(float alpha);

    
    ComponentPopupBuilder setBelongsToGlobalPopupStack(boolean isInStack);

    
    ComponentPopupBuilder setProject(Project project);

    
    ComponentPopupBuilder addUserData(Object object);

    
    ComponentPopupBuilder setModalContext(boolean modal);

    
    ComponentPopupBuilder setFocusOwners( Component[] focusOwners);

    /**
     * Adds "advertising" text to the bottom (e.g.: hints in code completion popup).
     */
    
    ComponentPopupBuilder setAdText( String text);

    
    ComponentPopupBuilder setAdText( String text, int textAlignment);

    
    ComponentPopupBuilder setShowShadow(boolean show);

    
    ComponentPopupBuilder setCommandButton( ActiveComponent commandButton);

    
    ComponentPopupBuilder setCouldPin( Processor<JBPopup> callback);

    
    ComponentPopupBuilder setKeyboardActions( List<Pair<ActionListener, KeyStroke>> keyboardActions);

    
    ComponentPopupBuilder setSettingButtons( Component button);

    
    ComponentPopupBuilder setMayBeParent(boolean mayBeParent);

    ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation);

    /**
     * Allows to define custom strategy for processing {@link JBPopup#dispatchKeyEvent(KeyEvent)}.
     */
    
    ComponentPopupBuilder setKeyEventHandler( BooleanFunction<KeyEvent> handler);

    
    ComponentPopupBuilder setShowBorder(boolean show);
}
