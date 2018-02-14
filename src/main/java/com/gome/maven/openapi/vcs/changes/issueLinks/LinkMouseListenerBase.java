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
package com.gome.maven.openapi.vcs.changes.issueLinks;

import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.SimpleColoredComponent;
import com.gome.maven.util.Consumer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public abstract class LinkMouseListenerBase<T> extends ClickListener implements MouseMotionListener {
    public static void installSingleTagOn( SimpleColoredComponent component) {
        new LinkMouseListenerBase<Object>() {
            
            @Override
            protected Object getTagAt( MouseEvent e) {
                //noinspection unchecked
                return ((SimpleColoredComponent)e.getSource()).getFragmentTagAt(e.getX());
            }

            @Override
            protected void handleTagClick( Object tag,  MouseEvent event) {
                if (tag != null) {
                    if (tag instanceof Consumer) {
                        //noinspection unchecked
                        ((Consumer<MouseEvent>)tag).consume(event);
                    }
                    else {
                        ((Runnable)tag).run();
                    }
                }
            }
        }.installOn(component);
    }

    
    protected abstract T getTagAt( MouseEvent e);

    @Override
    public boolean onClick( MouseEvent e, int clickCount) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            handleTagClick(getTagAt(e), e);
        }
        return false;
    }

    protected void handleTagClick( T tag,  MouseEvent event) {
        if (tag instanceof Runnable) {
            ((Runnable)tag).run();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Component component = (Component)e.getSource();
        Object tag = getTagAt(e);
        if (tag != null) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        else {
            component.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void installOn( Component component) {
        super.installOn(component);

        component.addMouseMotionListener(this);
    }
}
