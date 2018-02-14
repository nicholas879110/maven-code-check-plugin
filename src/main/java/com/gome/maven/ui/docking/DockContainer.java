/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.ui.docking;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.ui.awt.RelativeRectangle;
import com.gome.maven.util.ui.update.Activatable;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;

public interface DockContainer extends Disposable, Activatable {
    enum ContentResponse {ACCEPT_MOVE, ACCEPT_COPY, DENY;
        public boolean canAccept() {return this != DENY;}
    }

    RelativeRectangle getAcceptArea();

    /**
     * This area is used when nothing was found with getAcceptArea 
     */
    RelativeRectangle getAcceptAreaFallback();

    
    ContentResponse getContentResponse( DockableContent content, RelativePoint point);

    JComponent getContainerComponent();

    void add( DockableContent content, RelativePoint dropTarget);

    /**
     * Closes all contained editors.
     */
    void closeAll();

    void addListener(Listener listener, Disposable parent);

    boolean isEmpty();

    
    Image startDropOver( DockableContent content, RelativePoint point);

    
    Image processDropOver( DockableContent content, RelativePoint point);

    void resetDropOver( DockableContent content);


    boolean isDisposeWhenEmpty();

    interface Dialog extends DockContainer {}

    interface Persistent extends DockContainer {

        String getDockContainerType();
        Element getState();

    }

    interface Listener {
        void contentAdded(Object key);
        void contentRemoved(Object key);

        class Adapter implements Listener {
            @Override
            public void contentAdded(Object key) {
            }

            @Override
            public void contentRemoved(Object key) {
            }
        }
    }
}
