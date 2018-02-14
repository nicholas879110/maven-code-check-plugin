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
package com.gome.maven.ui.tabs;

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.ActiveRunnable;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.ui.switcher.SwitchProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public interface JBTabs extends SwitchProvider {

    
    TabInfo addTab(TabInfo info, int index);

    
    TabInfo addTab(TabInfo info);

    
    ActionCallback removeTab( TabInfo info);

    void removeAllTabs();

    
    JBTabs setPopupGroup( ActionGroup popupGroup,  String place, final boolean addNavigationGroup);

    
    ActionCallback select( TabInfo info, boolean requestFocus);

    
    TabInfo getSelectedInfo();

    
    TabInfo getTabAt(int tabIndex);

    int getTabCount();

    
    JBTabsPresentation getPresentation();

    
    DataProvider getDataProvider();

    
    TabInfo getTargetInfo();

    
    JBTabs addTabMouseListener( MouseListener listener);

    JBTabs addListener( TabsListener listener);

    JBTabs setSelectionChangeHandler(SelectionChangeHandler handler);

    @Override
    
    JComponent getComponent();

    
    TabInfo findInfo(MouseEvent event);

    
    TabInfo findInfo(Object object);

    int getIndexOf( final TabInfo tabInfo);

    void requestFocus();

    JBTabs setNavigationActionBinding(String prevActiobId, String nextActionId);
    JBTabs setNavigationActionsEnabled(boolean enabled);

    boolean isDisposed();

    JBTabs setAdditionalSwitchProviderWhenOriginal(SwitchProvider delegate);

    void resetDropOver(TabInfo tabInfo);
    Image startDropOver(TabInfo tabInfo, RelativePoint point);
    void processDropOver(TabInfo over, RelativePoint point);

    interface SelectionChangeHandler {
        
        ActionCallback execute(final TabInfo info, final boolean requestFocus,  ActiveRunnable doChangeSelection);
    }
}
