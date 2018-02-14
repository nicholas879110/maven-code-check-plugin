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

package com.gome.maven.execution.ui;

import com.gome.maven.execution.ui.layout.LayoutStateDefaults;
import com.gome.maven.execution.ui.layout.LayoutViewOptions;
import com.gome.maven.execution.ui.layout.PlaceInGrid;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.ComponentWithActions;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.ui.content.Content;
import com.gome.maven.ui.content.ContentManager;
import com.gome.maven.ui.content.ContentManagerListener;

import javax.swing.*;

public interface RunnerLayoutUi {

    
    LayoutStateDefaults getDefaults();

    
    LayoutViewOptions getOptions();

    
    ContentManager getContentManager();

    
    Content addContent( Content content);

    
    Content addContent( Content content, int defaultTabId,  PlaceInGrid defaultPlace, boolean defaultIsMinimized);

    
    Content createContent( String contentId,  JComponent component,  String displayName,  Icon icon,  JComponent toFocus);

    
    Content createContent( String contentId,  ComponentWithActions contentWithActions,  String displayName,  Icon icon,  JComponent toFocus);

    boolean removeContent( Content content, boolean dispose);

    
    Content findContent( String contentId);

    
    ActionCallback selectAndFocus( Content content, boolean requestFocus, final boolean forced);
    
    ActionCallback selectAndFocus( Content content, boolean requestFocus, final boolean forced, final boolean implicit);

    
    RunnerLayoutUi addListener( ContentManagerListener listener,  Disposable parent);

    void removeListener( final ContentManagerListener listener);

    void attractBy( String condition);
    void clearAttractionBy( String condition);

    void setBouncing( Content content, final boolean activate);

    
    JComponent getComponent();

    boolean isDisposed();

    void updateActionsNow();

    
    Content[] getContents();

    abstract class Factory {
        protected Factory() {
        }

        public static Factory getInstance(Project project) {
            return ServiceManager.getService(project, Factory.class);
        }

        
        public abstract RunnerLayoutUi create( String runnerId,  String runnerTitle,  String sessionName,  Disposable parent);
    }

}