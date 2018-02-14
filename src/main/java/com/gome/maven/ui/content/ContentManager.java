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
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.BusyObject;

import javax.swing.*;
import java.util.List;

public interface ContentManager extends Disposable, BusyObject {
    boolean canCloseContents();

    
    JComponent getComponent();

    void addContent( Content content);
    void addContent( Content content, final int order);
    void addContent( Content content, Object constraints);

    boolean removeContent( Content content, final boolean dispose);
    
    ActionCallback removeContent( Content content, final boolean dispose, boolean trackFocus, boolean forcedFocus);

    void setSelectedContent( Content content);
    
    ActionCallback setSelectedContentCB( Content content);
    void setSelectedContent( Content content, boolean requestFocus);
    
    ActionCallback setSelectedContentCB( Content content, boolean requestFocus);
    void setSelectedContent( Content content, boolean requestFocus, boolean forcedFocus);

    
    ActionCallback setSelectedContentCB( Content content, boolean requestFocus, boolean forcedFocus);

    
    ActionCallback setSelectedContent( Content content, boolean requestFocus, boolean forcedFocus, boolean implicit);

    void addSelectedContent( Content content);

    
    Content getSelectedContent();

    
    Content[] getSelectedContents();


    void removeAllContents(final boolean dispose);

    int getContentCount();

    
    Content[] getContents();

    //TODO[anton,vova] is this method needed?
    Content findContent(String displayName);

    
    Content getContent(int index);

    Content getContent(JComponent component);

    int getIndexOfContent(Content content);

    
    String getCloseActionName();

    boolean canCloseAllContents();

    ActionCallback selectPreviousContent();

    ActionCallback selectNextContent();

    void addContentManagerListener( ContentManagerListener l);

    void removeContentManagerListener( ContentManagerListener l);

    /**
     * Returns the localized name of the "Close All but This" action.
     *
     * @return the action name.
     * @since 5.1
     */
    
    String getCloseAllButThisActionName();

    
    String getPreviousContentActionName();

    
    String getNextContentActionName();

    List<AnAction> getAdditionalPopupActions(  Content content);

    void removeFromSelection( Content content);

    boolean isSelected( Content content);

    
    ActionCallback requestFocus( Content content, boolean forced);

    void addDataProvider( DataProvider provider);

    
    ContentFactory getFactory();

    boolean isDisposed();

    boolean isSingleSelection();
}
